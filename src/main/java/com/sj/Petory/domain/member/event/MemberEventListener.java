package com.sj.Petory.domain.member.event;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.sj.Petory.common.es.MemberDocument;
import com.sj.Petory.common.es.MemberEsRepository;
import com.sj.Petory.domain.member.entity.Member;
import com.sj.Petory.domain.member.repository.MemberRepository;
import com.sj.Petory.domain.member.type.MemberStatus;
import com.sj.Petory.domain.post.repository.PostEsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class MemberEventListener {

    private final MemberRepository memberRepository;
    private final MemberEsRepository memberEsRepository;
    private final PostEsRepository postEsRepository;
    private final ElasticsearchClient elasticsearchClient;

    @Scheduled(fixedDelay = 600000)
    public void memberSync() throws IOException {
        List<Member> members = memberRepository.findAllByStatus(MemberStatus.ACTIVE);

        List<MemberDocument> documents = members.stream()
                .map(member -> {
                    return MemberDocument.builder()
                            .memberId(member.getMemberId())
                            .name(member.getName())
                            .email(member.getEmail())
                            .build();
                }).toList();

        if (documents.isEmpty()) {
            System.out.println("✅ Member ES 동기화 대상 없음");
            return;
        }

        BulkRequest.Builder br = new BulkRequest.Builder();

        documents.forEach(doc ->
                br.operations(op -> op
                        .index(idx -> idx
                                .index("members")
                                .id(doc.getMemberId().toString())
                                .document(doc)
                        )
                )
        );
        elasticsearchClient.bulk(br.build());
        System.out.println("✅Member ES 동기화 완료: " + documents.size() + "건");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberUpdateEvent(MemberUpdatedEvent event) {
        log.info("MemberEventListener handleMemberUpdateEvent");

        memberEsRepository.findById(event.getMemberId())
                .ifPresent(doc -> {
                    MemberDocument updatedDoc = doc.updateName(event.getName());
                    memberEsRepository.save(updatedDoc);
                });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberDeletedEvent(MemberDeletedEvent event) {

        memberEsRepository.deleteById(event.getMemberId());
        postEsRepository.deleteByMemberId(event.getMemberId());
    }
}
