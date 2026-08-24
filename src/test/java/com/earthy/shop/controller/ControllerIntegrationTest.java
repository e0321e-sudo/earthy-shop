package com.earthy.shop.controller;

import com.earthy.shop.common.config.JwtUtil;
import com.earthy.shop.common.enums.UserRole;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.common.security.UserDetailsImpl;
import com.earthy.shop.domain.addon.controller.AddonController;
import com.earthy.shop.domain.addon.controller.AdminAddonController;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.admin.controller.AdminAuthController;
import com.earthy.shop.domain.admin.controller.AdminController;
import com.earthy.shop.domain.admin.dto.response.AdminLoginResponseDto;
import com.earthy.shop.domain.admin.service.AdminAuthService;
import com.earthy.shop.domain.admin.service.AdminService;
import com.earthy.shop.domain.board.controller.AdminBoardController;
import com.earthy.shop.domain.board.controller.BoardController;
import com.earthy.shop.domain.board.service.BoardService;
import com.earthy.shop.domain.cart.controller.CartController;
import com.earthy.shop.domain.cart.service.CartService;
import com.earthy.shop.domain.member.controller.AdminMemberController;
import com.earthy.shop.domain.member.controller.MemberAuthController;
import com.earthy.shop.domain.member.controller.MemberController;
import com.earthy.shop.domain.member.dto.response.MemberLoginResponseDto;
import com.earthy.shop.domain.member.enums.MemberStatusFilter;
import com.earthy.shop.domain.member.service.MemberAuthService;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.notice.controller.AdminNoticeController;
import com.earthy.shop.domain.notice.controller.NoticeController;
import com.earthy.shop.domain.notice.enums.NoticeVisibilityFilter;
import com.earthy.shop.domain.notice.service.NoticeService;
import com.earthy.shop.domain.oauth.controller.OAuthController;
import com.earthy.shop.domain.oauth.service.OAuthService;
import com.earthy.shop.domain.order.controller.AdminOrderController;
import com.earthy.shop.domain.order.controller.OrderController;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.order.service.OrderCancelService;
import com.earthy.shop.domain.order.service.OrderService;
import com.earthy.shop.domain.payment.controller.PaymentController;
import com.earthy.shop.domain.payment.service.PaymentService;
import com.earthy.shop.domain.product.controller.AdminProductController;
import com.earthy.shop.domain.product.controller.ProductController;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        ProductController.class,
        AdminProductController.class,
        AddonController.class,
        AdminAddonController.class,
        CartController.class,
        OrderController.class,
        AdminOrderController.class,
        PaymentController.class,
        MemberAuthController.class,
        MemberController.class,
        AdminMemberController.class,
        AdminAuthController.class,
        AdminController.class,
        BoardController.class,
        AdminBoardController.class,
        NoticeController.class,
        AdminNoticeController.class,
        OAuthController.class
})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "oauth.kakao.client-id=test-kakao-client-id",
        "oauth.kakao.redirect-uri=http://localhost:8080/api/oauth/kakao/callback",
        "oauth.kakao.frontend-redirect-uri=http://localhost:5175/auth"
})
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private ProductService productService;

    @MockBean
    private AddonService addonService;

    @MockBean
    private CartService cartService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderCancelService orderCancelService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private MemberAuthService memberAuthService;

    @MockBean
    private AdminAuthService adminAuthService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private BoardService boardService;

    @MockBean
    private NoticeService noticeService;

    @MockBean
    private OAuthService oAuthService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("고객 상품 목록 조회 API")
    void getProducts() throws Exception {
        // given
        given(productService.getProducts(eq(ProductCategory.POSTCARD), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("category", "POSTCARD")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청 성공"));

        verify(productService).getProducts(eq(ProductCategory.POSTCARD), any(Pageable.class));
    }

    @Test
    @DisplayName("고객 상품 상세 조회 API")
    void getProduct() throws Exception {
        // when & then
        mockMvc.perform(get("/api/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).getProduct(1L);
    }

    @Test
    @DisplayName("고객 상품명 검색 API")
    void searchProducts() throws Exception {
        // given
        given(productService.searchProducts(eq("sunset"), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "sunset")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).searchProducts(eq("sunset"), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 상품 등록 API")
    void createProduct() throws Exception {
        // given
        String request = """
                {
                  "name": "sunset sea postcard",
                  "category": "POSTCARD",
                  "price": 3500,
                  "imageUrl": "/assets/products/sunset-sea.jpeg",
                  "detailImageUrl": "/assets/products/detail.jpeg",
                  "description": "노을이 담긴 엽서",
                  "stockQuantity": 10
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 등록 성공"));

        verify(productService).createProduct(any());
    }

    @Test
    @DisplayName("관리자 상품 목록 조회 API")
    void getAdminProducts() throws Exception {
        // given
        given(productService.getAdminProducts(any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("요청 성공"));

        verify(productService).getAdminProducts(any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 상품 수정 API")
    void updateProduct() throws Exception {
        // given
        String request = """
                {
                  "name": "updated postcard",
                  "category": "POSTCARD",
                  "price": 4000,
                  "imageUrl": "/assets/products/sunset-sea.jpeg",
                  "detailImageUrl": "/assets/products/detail.jpeg",
                  "description": "수정된 설명",
                  "stockQuantity": 20
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/products/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 수정 성공"));

        verify(productService).updateProduct(eq(1L), any());
    }

    @Test
    @DisplayName("관리자 상품 판매 중지 API")
    void deactivateProduct() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/admin/products/{productId}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 비활성화 성공"));

        verify(productService).deactivateProduct(1L);
    }

    @Test
    @DisplayName("관리자 상품 판매 재개 API")
    void activateProduct() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/admin/products/{productId}/activate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 활성화 성공"));

        verify(productService).activateProduct(1L);
    }

    @Test
    @DisplayName("관리자 상품 삭제 API")
    void deleteProduct() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/admin/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 삭제 성공"));

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("고객 추가상품 목록 조회 API")
    void getAddons() throws Exception {
        // when & then
        mockMvc.perform(get("/api/addons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("요청 성공"));

        verify(addonService).getAddons();
    }

    @Test
    @DisplayName("관리자 추가상품 등록 API")
    void createAddon() throws Exception {
        // given
        String request = """
                {
                  "name": "A3 원목 액자",
                  "type": "FRAME",
                  "price": 12000,
                  "stockQuantity": 10
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("추가상품 등록 성공"));

        verify(addonService).createAddon(any());
    }

    @Test
    @DisplayName("장바구니 상품 담기 API")
    void addCartItem() throws Exception {
        // given
        String request = """
                {
                  "productId": 1,
                  "addonId": null,
                  "quantity": 2,
                  "addonQuantity": 0
                }
                """;

        // when & then
        mockMvc.perform(post("/api/cart")
                        .with(memberAuthentication())
                        .header("Idempotency-Key", "cart-idempotency-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("장바구니 상품 담기 성공"));

        verify(cartService).addCartItem(eq("member@example.com"), any(), eq("cart-idempotency-key"));
    }

    @Test
    @DisplayName("장바구니 수량 변경 API")
    void updateCartQuantity() throws Exception {
        // given
        String request = """
                {
                  "quantity": 3,
                  "addonQuantity": 1
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/cart/{cartItemId}", 1L)
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("장바구니 수량 변경 성공"));

        verify(cartService).updateQuantity(eq("member@example.com"), eq(1L), any());
    }

    @Test
    @DisplayName("장바구니 추가상품 수량 변경 API")
    void updateCartAddonQuantity() throws Exception {
        // given
        String request = """
                {
                  "quantity": 2
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/cart/{cartItemId}/addons/{cartItemAddonId}", 1L, 2L)
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("장바구니 추가상품 수량 변경 성공"));

        verify(cartService).updateAddonQuantity(eq("member@example.com"), eq(1L), eq(2L), any());
    }

    @Test
    @DisplayName("주문 생성 API")
    void createOrder() throws Exception {
        // given
        String request = """
                {
                  "cartItemIds": [1, 2],
                  "receiverName": "박수지",
                  "receiverPhone": "010-1234-5678",
                  "zipCode": "51100",
                  "address": "경남 창원시 소답동 148-3",
                  "detailAddress": "711호",
                  "deliveryMemo": "문 앞에 놓아주세요"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(memberAuthentication())
                        .header("Idempotency-Key", "test-idempotency-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주문 생성 성공"));

        verify(orderService).createOrder(eq("member@example.com"), any(), eq("test-idempotency-key"));
    }

    @Test
    @DisplayName("내 주문 목록 조회 API")
    void getMyOrders() throws Exception {
        // given
        given(orderService.getMyOrders(eq("member@example.com"), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/orders")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 주문 목록 조회 성공"));

        verify(orderService).getMyOrders(eq("member@example.com"), any(Pageable.class));
    }

    @Test
    @DisplayName("내 주문 취소 API")
    void cancelMyOrder() throws Exception {
        // given
        String request = """
                {
                  "cancelReason": "단순 변심"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", 1L)
                        .with(memberAuthentication())
                        .header("Idempotency-Key", "cancel-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주문 취소 성공"));

        verify(orderCancelService).cancelMyOrder("member@example.com", 1L, "단순 변심", "cancel-key");
    }

    @Test
    @DisplayName("관리자 주문 목록 조회 API")
    void getAdminOrders() throws Exception {
        // given
        given(orderService.getOrders(any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 주문 목록 조회 성공"));

        verify(orderService).getOrders(any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 주문 상태별 개수 조회 API")
    void getOrderStatusCounts() throws Exception {
        // given
        given(orderService.getOrderStatusCounts())
                .willReturn(Map.of(OrderStatus.PAID, 1L));

        // when & then
        mockMvc.perform(get("/api/admin/orders/status-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 주문 상태별 개수 조회 성공"))
                .andExpect(jsonPath("$.data.PAID").value(1));

        verify(orderService).getOrderStatusCounts();
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 API")
    void updateOrderStatus() throws Exception {
        // given
        String request = """
                {
                  "status": "SHIPPED",
                  "carrier": "우체국",
                  "trackingNumber": "1234567890"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/orders/{orderId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 주문 상태 변경 성공"));

        verify(orderService).updateOrderStatus(eq(1L), any());
    }

    @Test
    @DisplayName("결제 승인 API")
    void confirmPayment() throws Exception {
        // given
        String request = """
                {
                  "orderId": 1,
                  "paymentKey": "payment-key",
                  "amount": 6000,
                  "method": "카드"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .with(memberAuthentication())
                        .header("Idempotency-Key", "payment-confirm-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("결제 승인 성공"));

        verify(paymentService).confirmPayment(eq("member@example.com"), any(), eq("payment-confirm-key"));
    }

    @Test
    @DisplayName("회원가입 API")
    void signup() throws Exception {
        // given
        String request = """
                {
                  "email": "member@example.com",
                  "password": "password123!",
                  "name": "박수지",
                  "phone": "010-1234-5678",
                  "termsAgreed": true,
                  "privacyAgreed": true,
                  "marketingAgreed": false
                }
                """;

        // when & then
        mockMvc.perform(post("/api/member/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 성공"));

        verify(memberService).signup(any());
    }

    @Test
    @DisplayName("회원가입 API 요청값 검증 실패")
    void signupValidationFail() throws Exception {
        // given
        String request = """
                {
                  "email": "invalid-email",
                  "password": "password123!",
                  "name": "박수지",
                  "phone": "010-1234-5678",
                  "termsAgreed": true,
                  "privacyAgreed": true
                }
                """;

        // when & then
        mockMvc.perform(post("/api/member/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("회원 로그인 API")
    void memberLogin() throws Exception {
        // given
        given(memberAuthService.login(any()))
                .willReturn(new MemberLoginResponseDto("access-token", "refresh-token"));

        String request = """
                {
                  "email": "member@example.com",
                  "password": "password123!"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/member/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));

        verify(memberAuthService).login(any());
    }

    @Test
    @DisplayName("내 정보 조회 API")
    void getMyInfo() throws Exception {
        // when & then
        mockMvc.perform(get("/api/member/me")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 정보 조회 성공"));

        verify(memberService).getMyInfo("member@example.com");
    }

    @Test
    @DisplayName("내 정보 수정 API")
    void updateMyInfo() throws Exception {
        // given
        String request = """
                {
                  "name": "홍길동",
                  "phone": "010-5236-6666",
                  "zipCode": "13529",
                  "address": "경기 성남시 분당구 판교역로 166",
                  "detailAddress": "102호"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/member/me")
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 정보 수정 성공"));

        verify(memberService).updateMyInfo(eq("member@example.com"), any());
    }

    @Test
    @DisplayName("관리자 로그인 API")
    void adminLogin() throws Exception {
        // given
        given(adminAuthService.login(any()))
                .willReturn(new AdminLoginResponseDto("admin-access-token", "admin-refresh-token"));

        String request = """
                {
                  "email": "earthy@gmail.com",
                  "password": "password123!"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("admin-access-token"));

        verify(adminAuthService).login(any());
    }

    @Test
    @DisplayName("관리자 회원 목록 조회 API")
    void getAdminMembers() throws Exception {
        // given
        given(memberService.getAdminMembers(eq(MemberStatusFilter.ACTIVE), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/admin/members")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 회원 목록 조회 성공"));

        verify(memberService).getAdminMembers(eq(MemberStatusFilter.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 비밀번호 변경 API")
    void updateAdminPassword() throws Exception {
        // given
        String request = """
                {
                  "currentPassword": "old-password",
                  "newPassword": "new-password123!"
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/password")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 비밀번호 변경 성공"));

        verify(adminService).updatePassword(eq("admin@example.com"), any());
    }

    @Test
    @DisplayName("고객 게시글 목록 조회 API")
    void getBoards() throws Exception {
        // given
        given(boardService.getBoards(eq("문의"), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/boards")
                        .param("keyword", "문의"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 목록 조회 성공"));

        verify(boardService).getBoards(eq("문의"), any(Pageable.class));
    }

    @Test
    @DisplayName("고객 게시글 작성 API")
    void createBoard() throws Exception {
        // given
        String request = """
                {
                  "type": "PRODUCT",
                  "title": "문의합니다.",
                  "content": "상품 문의 내용",
                  "visibility": "PUBLIC",
                  "postPassword": null
                }
                """;

        // when & then
        mockMvc.perform(post("/api/boards")
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 작성 성공"));

        verify(boardService).createBoard(eq("member@example.com"), any());
    }

    @Test
    @DisplayName("비공개 게시글 비밀번호 확인 API")
    void checkPrivateBoardPassword() throws Exception {
        // given
        String request = """
                {
                  "postPassword": "1234"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/boards/{boardId}/password", 1L)
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 상세 조회 성공"));

        verify(boardService).getPrivateBoard(eq("member@example.com"), eq(1L), any());
    }

    @Test
    @DisplayName("관리자 게시글 답변 등록 API")
    void answerBoard() throws Exception {
        // given
        String request = """
                {
                  "answer": "문의 답변입니다."
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/boards/{boardId}/answer", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 답변 등록 성공"));

        verify(boardService).answerBoard(eq(1L), any());
    }

    @Test
    @DisplayName("고객 공지 목록 조회 API")
    void getNotices() throws Exception {
        // given
        given(noticeService.getNotices(eq("배송"), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/notices")
                        .param("keyword", "배송"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 목록 조회 성공"));

        verify(noticeService).getNotices(eq("배송"), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 공지 목록 조회 API")
    void getAdminNotices() throws Exception {
        // given
        given(noticeService.getAdminNotices(eq("배송"), eq(NoticeVisibilityFilter.PUBLIC), any(Pageable.class)))
                .willReturn(PageResponseDto.from(Page.empty(PageRequest.of(0, 20))));

        // when & then
        mockMvc.perform(get("/api/admin/notices")
                        .param("keyword", "배송")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관리자 공지사항 목록 조회 성공"));

        verify(noticeService).getAdminNotices(eq("배송"), eq(NoticeVisibilityFilter.PUBLIC), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 공지 등록 API")
    void createNotice() throws Exception {
        // given
        String request = """
                {
                  "title": "배송비 및 교환/환불 공지",
                  "content": "공지 내용",
                  "visible": true
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 등록 성공"));

        verify(noticeService).createNotice(any());
    }

    @Test
    @DisplayName("카카오 로그인 시작 API")
    void redirectToKakaoLogin() throws Exception {
        // when & then
        mockMvc.perform(get("/api/oauth/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://kauth.kakao.com/oauth/authorize")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=test-kakao-client-id")));
    }

    @Test
    @DisplayName("카카오 로그인 콜백 API")
    void kakaoCallback() throws Exception {
        // given
        given(oAuthService.loginWithKakao("kakao-code"))
                .willReturn(new MemberLoginResponseDto("access-token", "refresh-token"));

        // when & then
        mockMvc.perform(get("/api/oauth/kakao/callback")
                        .param("code", "kakao-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("http://localhost:5175/auth")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("oauth=kakao")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("accessToken=access-token")));

        verify(oAuthService).loginWithKakao("kakao-code");
    }

    private static RequestPostProcessor memberAuthentication() {
        UserDetailsImpl userDetails = new UserDetailsImpl("member@example.com", UserRole.MEMBER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        return request -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private static RequestPostProcessor adminAuthentication() {
        UserDetailsImpl userDetails = new UserDetailsImpl("admin@example.com", UserRole.ADMIN);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        return request -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            request.setUserPrincipal(authentication);
            return request;
        };
    }
}
