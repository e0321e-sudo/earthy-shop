package com.earthy.shop.domain.member.dto.response;

import com.earthy.shop.common.enums.LoginProvider;
import com.earthy.shop.domain.member.entity.Member;

import java.util.List;

// 이메일 찾기 응답 DTO
public record MemberEmailFindResponseDto(
        String email,
        LoginProvider provider,
        String providerDescription,
        List<Account> accounts
) {
    public static MemberEmailFindResponseDto from(List<Member> members) {
        Member firstMember = members.getFirst();
        Account firstAccount = Account.from(firstMember);

        return new MemberEmailFindResponseDto(
                firstAccount.email(),
                firstAccount.provider(),
                firstAccount.providerDescription(),
                members.stream()
                        .map(Account::from)
                        .toList()
        );
    }

    public record Account(
            String email,
            LoginProvider provider,
            String providerDescription
    ) {
        public static Account from(Member member) {
            LoginProvider provider = member.getProvider() == null
                    ? LoginProvider.LOCAL
                    : member.getProvider();

            return new Account(
                    member.getEmail(),
                    provider,
                    provider.getDescription()
            );
        }
    }

    public static MemberEmailFindResponseDto from(Member member) {
        LoginProvider provider = member.getProvider() == null
                ? LoginProvider.LOCAL
                : member.getProvider();

        return new MemberEmailFindResponseDto(
                member.getEmail(),
                provider,
                provider.getDescription(),
                List.of(Account.from(member))
        );
    }
}
