# Prompt chuẩn hóa giao diện Library Management System

> Trích nguyên văn từ lịch sử Codex ngày 18/07/2026.  
> Thay phần `[DANH SÁCH 3–5 TEMPLATE CẦN XỬ LÝ]` trước khi giao cho AI khác.

Bạn đang làm việc trên dự án Spring Boot + Thymeleaf Library Management System.

NHIỆM VỤ

Chuẩn hóa giao diện các trang được chỉ định theo hệ thống thiết kế hiện tại của dự án, đồng thời giữ nguyên hoàn toàn nghiệp vụ và code của các thành viên khác.

Chỉ xử lý các trang sau:

[DANH SÁCH 3–5 TEMPLATE CẦN XỬ LÝ]

Ví dụ:

- src/main/resources/templates/member/favorites.html
- src/main/resources/templates/member/reviews.html
- src/main/resources/templates/member/book-acquisition-request.html
- src/main/resources/templates/member/notifications.html

==================================================
I. CÁC NGUỒN GIAO DIỆN CHUẨN
==================================================

Sử dụng các nguồn chuẩn theo thứ tự sau:

1. Visual design tổng thể

- src/main/resources/templates/admin/dashboard.html
- src/main/resources/static/css/pages/admin/dashboard.css

Dùng Admin Dashboard làm chuẩn về:

- Phong cách tổng thể.
- Font chữ.
- Hệ thống khoảng cách.
- Màu sắc.
- Card.
- Button.
- Shadow.
- Border.
- Banner.
- Mật độ thông tin.
- Cảm giác giao diện.

2. Banner/Hero của các trang Member

Dùng banner của trang:

- src/main/resources/templates/member/notifications.html
- src/main/resources/static/css/pages/member/notifications.css

làm chuẩn về:

- Chiều rộng.
- Chiều cao.
- Padding.
- Border radius.
- Gradient.
- Shadow.
- Vị trí họa tiết tròn.
- Font tiêu đề.
- Kích thước tiêu đề.
- Eyebrow.
- Mô tả.
- Vị trí action button.

Tất cả banner cùng nhóm trang phải có cùng kích thước và typography, không được mỗi trang một cỡ.

3. Danh sách và bảng dữ liệu

Dùng phần danh sách “Tất cả thông báo” của:

- src/main/resources/templates/member/notifications.html

làm chuẩn cho các danh sách dạng card hoặc bảng:

- Khung danh sách bên ngoài.
- Header.
- Khoảng cách giữa các hàng.
- Padding.
- Màu nền.
- Border.
- Border radius.
- Shadow.
- Hiệu ứng hover.
- Empty state.
- Pagination.
- Mật độ thông tin.

Các item phải được bo đủ bốn góc.

Không được chỉ bo hai góc hoặc dùng đường phân cách khiến item trông như bị cắt.

4. Toast/feedback message

Dùng toast khi thêm hoặc bỏ sách yêu thích làm chuẩn cho các thông báo thao tác:

- Thành công.
- Thất bại.
- Cảnh báo.
- Thông tin.

Tất cả thông báo kiểu “đã thêm yêu thích”, “đã xóa đánh giá”, “đã gửi đề xuất”, “cập nhật thành công”… phải dùng cùng một hệ thống toast.

Ưu tiên sử dụng component hoặc hàm chung hiện có như AppFeedback.

Không tạo mỗi trang một loại alert/toast riêng.

5. Validation

Dùng validation của form “Gửi đề xuất mới” tại:

- src/main/resources/templates/member/book-acquisition-request.html
- src/main/resources/static/css/pages/member/book-acquisition-request.css

làm chuẩn về:

- Màu chữ lỗi.
- Độ đậm.
- Font size.
- Line-height.
- Khoảng cách với input.
- Viền input lỗi.
- Focus ring của input lỗi.
- Cách hiển thị lỗi server-side và client-side.

Không tự thiết kế một validation style mới nếu style chuẩn đã có.

6. Modal/pop-up

Dùng modal “Gửi đề xuất mới” đã chuẩn hóa làm chuẩn về:

- Overlay.
- Modal container.
- Border.
- Border radius.
- Shadow.
- Header.
- Body.
- Footer.
- Padding.
- Typography.
- Button.
- Vị trí thông báo validation.
- Giới hạn chiều cao và vùng scroll.

==================================================
II. QUY TẮC AN TOÀN — KHÔNG ĐƯỢC LÀM MẤT CODE
==================================================

Đây là yêu cầu bắt buộc, không phải khuyến nghị.

1. Không được thay đổi, ghi đè hoặc xóa code làm mất nghiệp vụ hiện có.

2. Trước khi sửa một trang, phải đọc đầy đủ:

- Layout đang sử dụng.
- Template liên quan.
- CSS riêng của trang.
- CSS dùng chung.
- JavaScript liên quan.
- Controller.
- DTO/form object.
- Service.
- Repository nếu có phân trang hoặc bộ lọc.
- Test liên quan.

3. Phải giữ nguyên:

- Endpoint.
- URL.
- HTTP method.
- Form action.
- Form method.
- CSRF token.
- Thymeleaf expression.
- th:if.
- th:unless.
- th:each.
- th:object.
- th:field.
- th:action.
- Validation binding.
- Redirect flow.
- Flash attribute.
- Phân trang.
- Bộ lọc.
- Quyền truy cập.
- ID được JavaScript sử dụng.
- Class được JavaScript sử dụng.
- data-* được JavaScript sử dụng.
- aria-* có ý nghĩa chức năng.

4. Nếu thay đổi class, ID hoặc cấu trúc DOM có JavaScript sử dụng, phải cập nhật đồng bộ JavaScript và kiểm tra lại toàn bộ luồng.

5. Không được chọn toàn bộ “ours” hoặc “theirs” khi xử lý conflict.

Phải đọc cả hai phía và hợp nhất:

- Giữ nghiệp vụ mới của thành viên khác.
- Giữ hệ thống giao diện và i18n hiện có.
- Giữ validation.
- Giữ JavaScript.
- Không làm mất endpoint hoặc dữ liệu hiển thị.

6. Không sử dụng:

- git reset --hard
- git checkout -- file
- thao tác xóa hoặc khôi phục có thể làm mất code

trừ khi người dùng yêu cầu rõ ràng.

7. Trước khi xóa CSS, class, ID, fragment hoặc JavaScript:

- Dùng rg tìm toàn bộ project.
- Xác nhận không còn template hoặc JavaScript nào tham chiếu.
- Chỉ xóa khi chắc chắn không ảnh hưởng chức năng.

8. Không tự ý sửa backend hoặc database.

Nếu giao diện không thể hoàn thiện vì thiếu dữ liệu từ backend:

- Báo rõ dữ liệu đang thiếu.
- Đề xuất thay đổi.
- Không tự ý sửa controller, service, repository, entity hoặc database nếu chưa được cho phép.

9. Không tạo file HTML hoặc CSS mới nếu trang đã có file tương ứng.

Phải ưu tiên sửa và tái sử dụng file hiện có.

==================================================
III. I18N ANH–VIỆT
==================================================

1. Không viết cứng chuỗi giao diện mới trong HTML hoặc JavaScript.

2. Mọi nội dung giao diện mới phải có đầy đủ trong:

- src/main/resources/messages.properties
- src/main/resources/messages_vi.properties

3. Tiếng Anh là ngôn ngữ mặc định.

4. Phải kiểm tra cả English và Vietnamese để tránh:

- Tràn chữ.
- Button quá nhỏ.
- Header lệch.
- Pill sai chiều cao.
- Cột bị vỡ.
- Modal bị tràn.
- Tiêu đề xuống dòng bất hợp lý.

5. Không tự dịch dữ liệu nghiệp vụ do người dùng nhập, ví dụ:

- Tên sách.
- Tác giả.
- Nội dung đánh giá.
- Lý do đề xuất.
- Nội dung thông báo thủ thư nhập thủ công.

Chỉ dịch label, message key, validation và nội dung hệ thống.

6. Nội dung toast, modal, button, empty state, validation và aria-label mới đều phải qua i18n.

==================================================
IV. BẢNG MÀU CHUẨN
==================================================

Sử dụng bảng màu sau:

- 50:  #FCEEE7
- 100: #F9D7C3
- 200: #F5B27F
- 300: #E2954C
- 400: #C38040
- 500: #A66C35
- 600: #8B5A2B
- 700: #724922
- 800: #513215
- 900: #2E1A08
- 950: #1B0E03

Quy tắc sử dụng:

1. Nền trang chính vẫn là màu trắng.

2. Không dùng nền hồng phủ toàn bộ card, bảng hoặc list.

3. Palette chỉ được dùng cho:

- Primary action.
- Active state.
- Hover.
- Focus.
- Border nhấn.
- Pill.
- Icon.
- Hero/banner.
- Pagination active.
- Accent nhỏ.

4. Ưu tiên dùng CSS variables/design tokens dùng chung.

5. Không khai báo màu riêng tại từng trang nếu đã có token tương ứng.

6. Không tự tạo thêm màu mới nếu palette hiện có đáp ứng được.

Nếu bắt buộc phải bổ sung màu vì:

- Success.
- Warning.
- Error.
- Accessibility.
- Contrast.

thì phải giải thích rõ trước hoặc trong báo cáo cuối.

7. Không dùng màu nền nhạt phủ quá rộng khiến giao diện bị hồng hoặc cam toàn bộ.

==================================================
V. TYPOGRAPHY VÀ KÍCH THƯỚC
==================================================

1. Font UI, display font, font size, line-height và font-weight phải lấy từ component chuẩn.

2. Tiêu đề của các banner phải đồng nhất giữa các trang.

3. Header của bảng/list phải đồng nhất:

- Font family.
- Font size.
- Font weight.
- Line-height.
- Letter spacing.
- Uppercase.
- Padding trên/dưới.
- Alignment.

Số lượng cột có thể khác nhau nhưng typography và chiều cao header không được khác nhau.

Ví dụ hai dãy sau phải cùng chuẩn typography:

- Tên sách – Tác giả – Trạng thái – Thao tác
- Tên sách – Tác giả – Ngày đánh giá – Đánh giá – Thao tác

4. Không dùng selector quá tổng quát làm thay đổi font của toàn bộ trang ngoài phạm vi.

5. Không dùng inline style.

6. Hạn chế tối đa `!important`.

Chỉ dùng nếu đã kiểm tra specificity và không còn phương án an toàn hơn.

==================================================
VI. BANNER VÀ TIÊU ĐỀ TRANG
==================================================

1. Các banner trong cùng nhóm phải có:

- Cùng chiều cao.
- Cùng padding.
- Cùng border radius.
- Cùng kích thước tiêu đề.
- Cùng vị trí nội dung.
- Cùng shadow.
- Cùng họa tiết trang trí.

2. Tiêu đề dài phải co giãn hoặc xuống dòng hợp lý nhưng không làm banner khác kích thước.

3. Nếu banner đã thể hiện rõ tên trang thì không lặp lại tiêu đề ngay bên dưới.

Ví dụ không lặp lại các dòng:

- Đánh giá đã gửi.
- Gửi đề xuất mới.
- Tất cả thông báo.

Chỉ giữ lại nếu nó thực sự là tiêu đề của một subsection khác biệt và cần thiết cho accessibility.

4. Nếu bỏ heading hiển thị, vẫn phải giữ aria-label hoặc heading ẩn phù hợp nếu cần cho semantic HTML.

==================================================
VII. LIST, TABLE VÀ CARD
==================================================

1. Các danh sách dạng bảng/card phải lấy “Tất cả thông báo” làm chuẩn.

2. Mỗi item phải:

- Có đủ bốn góc bo.
- Có border đồng nhất.
- Có background trắng.
- Có padding đồng nhất.
- Có shadow nhẹ đồng nhất.
- Có khoảng cách giữa các item đồng nhất.
- Không dính sát nhau.

3. Hover của mọi item phải giống hover của Notification:

- Cùng border-color.
- Cùng shadow.
- Cùng transform.
- Cùng transition.
- Không trang đổi nền, trang khác chỉ đổi viền.

4. Không để dữ liệu tùy chọn làm thay đổi bố cục cột cố định.

Ví dụ các trường:

- ISBN.
- Publisher.
- Publication year.
- Ghi chú.
- Metadata tùy chọn.

phải nằm trong vùng nội dung của item.

Chúng không được chiếm một cột Grid độc lập rồi đẩy:

- Trạng thái.
- Ngày.
- Thời gian.
- Thao tác.

sang hàng hoặc vị trí khác.

5. Trong cùng một danh sách:

- Trạng thái luôn ở cùng vị trí.
- Ngày/giờ luôn ở cùng vị trí.
- Action luôn ở cùng vị trí.
- Dữ liệu tùy chọn chỉ mở rộng vùng nội dung.

6. Empty state phải dùng cùng:

- Padding.
- Icon size.
- Font.
- Màu.
- Border.
- Border radius.
- Alignment.

7. Pagination phải dùng component chung và có:

- Normal.
- Hover.
- Active.
- Disabled.
- Focus-visible.

==================================================
VIII. PILL/BADGE
==================================================

Tất cả pill/badge trong cùng hệ thống phải dùng chung component hoặc class dùng chung.

Áp dụng cho:

- Thể loại sách.
- Trạng thái sách.
- Rating.
- Loại thông báo.
- Chưa đọc.
- Trạng thái đề xuất.
- Các metadata dạng viên thuốc.

Quy tắc:

1. Chiều ngang có thể khác nhau theo độ dài chữ.

2. Chiều cao phải tuyệt đối đồng nhất.

3. Dùng chung:

- Height.
- Min-height.
- Max-height.
- Padding ngang.
- Border radius.
- Font family.
- Font size.
- Font weight.
- Line-height.
- Border color.
- Background.
- Text color.

4. Không được khai báo màu pill riêng tại từng trang nếu không có yêu cầu nghiệp vụ đặc biệt.

5. Pill hiện tại dùng chung màu từ palette:

- Nền lấy từ palette 200 ở mức opacity nhẹ.
- Viền lấy từ palette 400 ở mức opacity phù hợp.
- Chữ lấy từ palette 800.

6. Nếu cần semantic status khác màu như error/rejected, chỉ tạo modifier dùng chung; không viết style riêng trong từng trang.

==================================================
IX. BUTTON VÀ INTERACTION
==================================================

Tất cả button trong các trang được xử lý phải dùng chung hệ thống variant:

- Primary.
- Secondary.
- Danger.
- Inverse.
- Text action.
- Icon button.
- Pagination control.

Mỗi variant phải có đủ:

- Normal.
- Hover.
- Active/pressed.
- Focus-visible.
- Disabled.
- Loading nếu có.

Quy tắc:

1. Không tạo button style riêng cho từng trang nếu component chung đã có.

2. Button cùng chức năng phải cùng:

- Height.
- Padding.
- Border radius.
- Font.
- Màu.
- Border.
- Shadow.
- Hover.
- Active.
- Focus.

3. Hover và active phải nhìn khác nhau rõ ràng.

4. Active nên có phản hồi nhấn nhẹ, ví dụ:

- Translate xuống nhẹ.
- Inset shadow.
- Màu đậm hơn.

5. Focus-visible phải đủ tương phản và hỗ trợ keyboard.

6. Disabled phải:

- Không có hover.
- Không có active transform.
- Hiển thị rõ là không thao tác được.

==================================================
X. MODAL/POP-UP
==================================================

1. Form lớn như “Gửi đề xuất mới” không nên chiếm toàn bộ nội dung trang nếu chỉ là thao tác bổ sung.

Cách chuẩn:

- Có một button “Gửi đề xuất”.
- Bấm button mở modal.
- Form nằm trong modal.
- Danh sách vẫn là nội dung chính của trang.

2. Modal phải có cấu trúc:

- Header.
- Body có thể scroll.
- Footer cố định.
- Secondary/Cancel button.
- Primary hoặc Danger action.

3. Nếu modal đã có nút “Hủy/Cancel” rõ ràng ở footer thì không hiển thị thêm nút X đóng ở header.

4. Không để đồng thời cả nút X và nút Hủy nếu chúng cùng chức năng.

5. Modal phải:

- Không làm mất giá trị form khi validation lỗi.
- Tự mở lại nếu server trả validation error.
- Focus vào trường lỗi đầu tiên.
- Giữ nguyên CSRF.
- Giữ nguyên form method/action.
- Không phá th:field.
- Không phá server-side validation.
- Không phá client-side validation.

6. Delete confirmation phải dùng chung modal style nhưng action nguy hiểm dùng danger variant.

7. Edit modal và delete modal phải cùng:

- Border radius.
- Header spacing.
- Footer spacing.
- Shadow.
- Overlay.
- Typography.
- Button height.

8. Không dùng `window.confirm()` hoặc popup trình duyệt nếu dự án đã có modal chuẩn.

==================================================
XI. VALIDATION FORM
==================================================

1. Lấy giao diện validation của form “Gửi đề xuất mới” làm chuẩn.

2. Phải giữ nguyên:

- Màu chữ lỗi.
- Độ đậm.
- Font size.
- Line-height.
- Viền input lỗi.
- Focus ring lỗi.
- Khoảng cách giữa field và error.
- Khoảng cách giữa các field.

3. Không tạo validation style mới tại từng trang.

4. Error message server-side và client-side phải dùng cùng giao diện.

5. Với textarea có bộ đếm ký tự:

- Error nằm bên trái.
- Character counter nằm cố định bên phải.
- Cùng một hàng hỗ trợ.
- Không để counter bị đẩy lệch bởi error dài.
- Không dùng inline style.

Ví dụ:

[Lý do đề xuất không được để trống.]                 [0/1000]

6. Không thay đổi nội dung hoặc quy tắc validation nghiệp vụ nếu nhiệm vụ chỉ là sửa giao diện.

==================================================
XII. TOAST, FEEDBACK VÀ THÔNG BÁO THAO TÁC
==================================================

1. Dùng toast thêm/bỏ yêu thích làm chuẩn.

2. Tất cả feedback thao tác phải qua component chung như `AppFeedback`.

3. Toast phải có cấu trúc thống nhất:

- Icon.
- Tiêu đề ngắn.
- Nội dung.
- Nút đóng nếu component chuẩn có.
- Border trái hoặc semantic indicator.
- Shadow.
- Border radius.
- Vị trí hiển thị nhất quán.

4. Hỗ trợ các loại:

- Success.
- Error.
- Warning.
- Information.

5. Không dùng lẫn lộn:

- Inline alert ở trang A.
- Browser alert ở trang B.
- Toast ở trang C.

cho cùng một loại feedback thao tác.

6. Không dùng `window.alert()` nếu đã có toast chung.

7. Form validation error vẫn hiển thị cạnh field; không biến validation field thành toast.

8. Toast phải lấy nội dung từ i18n hoặc message backend, không viết cứng.

9. Không tạo thêm thư viện toast nếu project đã có component chung đáp ứng được.

==================================================
XIII. FILTER VÀ PAGINATION
==================================================

1. Không được làm hỏng bộ lọc hiện có khi sửa giao diện.

2. Phải kiểm tra toàn bộ chuỗi:

- Query parameter.
- Controller parsing.
- Service.
- Repository query.
- Model attribute.
- Selected state trong HTML.
- JavaScript navigation.
- Pagination URL.

3. Bộ lọc kết hợp phải giữ đúng logic AND nếu nghiệp vụ yêu cầu.

Ví dụ Notification:

- source=all + type=ALL: tất cả thông báo.
- source=librarian + type=ALL: tất cả thông báo từ thủ thư.
- source=system + type=LOAN: chỉ thông báo mượn sách từ hệ thống.

4. Khi bấm “All Notifications”, phải quay về toàn bộ nguồn và toàn bộ loại nếu đó là ý nghĩa của tab.

5. Nếu bộ lọc không có kết quả:

- Hiển thị empty state rõ ràng.
- Có nút “Clear Filters/Xóa bộ lọc”.
- Không làm người dùng tưởng dữ liệu đã bị mất.

6. Khi thay đổi filter:

- Đưa page về 0.
- Giữ ngôn ngữ hiện tại.
- Xóa query parameter không còn phù hợp.
- Không mất các parameter cần thiết khác.

7. Danh sách nghiệp vụ mới nhất phải hiển thị trước nếu người dùng cần xử lý mới nhất trước.

Ví dụ đề xuất bổ sung sách của thủ thư phải sắp xếp:

- createdDate DESC.
- requestId DESC để làm tie-breaker.

Không được dùng thứ tự tăng dần khiến bản ghi mới bị đẩy sang trang cuối.

==================================================
XIV. RESPONSIVE
==================================================

Ở giai đoạn hiện tại, ưu tiên giao diện desktop.

Không cần dành thêm thời gian tối ưu hoặc kiểm thử riêng cho mobile nếu người dùng không yêu cầu.

Tuy nhiên:

- Không được cố tình thêm CSS làm hỏng responsive hiện có.
- Không xóa media query cũ nếu chưa xác nhận không cần.
- Không tạo overflow ngang mới trên desktop.
- English và Vietnamese vẫn phải hiển thị đúng trên desktop.

==================================================
XV. CSS ARCHITECTURE
==================================================

1. Ưu tiên tái sử dụng:

- Design tokens.
- CSS variables.
- Component class dùng chung.
- Layout hiện có.
- Fragment hiện có.

2. Không tạo lại cùng một rule trong bốn file CSS khác nhau.

3. Quy tắc dùng chung phải đặt ở CSS dùng chung phù hợp.

4. CSS riêng của trang chỉ chứa:

- Layout đặc thù.
- Component thực sự chỉ trang đó sử dụng.
- Override có phạm vi rõ ràng.

5. Không dùng selector quá rộng làm ảnh hưởng trang ngoài phạm vi.

6. Không xóa class cũ trước khi xác nhận không còn HTML hoặc JavaScript sử dụng.

7. Không tải thêm framework hoặc thư viện UI nếu chưa được yêu cầu.

==================================================
XVI. ACCESSIBILITY
==================================================

1. Giữ hoặc bổ sung hợp lý:

- aria-label.
- aria-labelledby.
- aria-describedby.
- aria-live.
- aria-expanded.
- aria-selected.
- role phù hợp.

2. Modal phải quản lý focus hợp lý.

3. Button icon-only phải có accessible label.

4. Focus-visible phải nhìn thấy rõ.

5. Không dùng div giả button nếu có thể dùng button thật.

6. Không phá semantic table/list hiện tại chỉ để dễ CSS.

==================================================
XVII. QUY TRÌNH BẮT BUỘC TRƯỚC KHI SỬA
==================================================

Trước khi sửa, hãy:

1. Chạy `git status --short`.

2. Xác định code đang sửa dở của người khác.

3. Đọc toàn bộ template trong phạm vi.

4. Đọc layout mà template kế thừa.

5. Đọc CSS riêng và CSS chung liên quan.

6. Tìm JavaScript tham chiếu tới ID, class và data-*.

7. Đọc controller/service/repository liên quan ở chế độ chỉ đọc.

8. Dùng `rg` kiểm tra class/ID trước khi đổi hoặc xóa.

9. So sánh với component chuẩn bằng code, không chỉ dựa vào ảnh.

10. Xác định rõ:

- Phần nào là nghiệp vụ.
- Phần nào chỉ là trình bày.
- Phần nào đang được JavaScript sử dụng.
- Phần nào có thể tái sử dụng.

==================================================
XVIII. CÁCH TRIỂN KHAI
==================================================

Thực hiện theo từng nhóm nhỏ, tối đa 3–5 template mỗi lần.

Thứ tự đề xuất:

1. Chuẩn hóa shared tokens/component trước.

2. Chuẩn hóa banner.

3. Chuẩn hóa list/table/card.

4. Chuẩn hóa pill.

5. Chuẩn hóa button.

6. Chuẩn hóa modal.

7. Chuẩn hóa validation.

8. Chuẩn hóa toast/feedback.

9. Kiểm tra i18n.

10. Kiểm tra nghiệp vụ và JavaScript.

Không thực hiện rewrite toàn bộ template nếu chỉ cần chỉnh cấu trúc và class.

Không format hoặc sửa những file không liên quan.

==================================================
XIX. KIỂM TRA SAU KHI SỬA
==================================================

Sau khi sửa phải thực hiện:

1. Kiểm tra `git diff`.

2. Kiểm tra chỉ những file trong phạm vi hoặc file shared thực sự cần thiết được thay đổi.

3. Chạy:

- `git diff --check`
- compile project
- test render liên quan
- test controller/service liên quan nếu có filter, pagination hoặc form
- kiểm tra Thymeleaf không phát sinh TemplateInputException

4. Kiểm tra thủ công tối thiểu:

- English.
- Vietnamese.
- Trang có dữ liệu.
- Empty state.
- Pagination.
- Filter.
- Validation lỗi.
- Submit thành công.
- Submit thất bại.
- Modal open/close.
- Toast.
- Hover.
- Active.
- Focus.
- Disabled.

5. Nếu sửa ID/class có JavaScript sử dụng, phải test lại JavaScript tương ứng.

6. Không tuyên bố “hoàn thành 100%” nếu chưa kiểm tra đầy đủ.

==================================================
XX. BÁO CÁO KẾT QUẢ
==================================================

Sau khi hoàn thành, phải liệt kê rõ:

1. File đã sửa.

2. Mỗi file sửa gì.

3. Component dùng chung nào được tái sử dụng.

4. i18n key nào được thêm hoặc thay đổi.

5. Nghiệp vụ nào đã được giữ nguyên.

6. Endpoint/form/CSRF/JavaScript nào đã được xác nhận không bị hỏng.

7. Test nào đã chạy.

8. Kết quả compile.

9. Kết quả `git diff --check`.

10. Những phần chưa kiểm tra được hoặc còn rủi ro.

11. Nếu phát hiện code thừa, chỉ báo lại; không tự xóa khi chưa được cho phép.

==================================================
XXI. TIÊU CHÍ HOÀN THÀNH
==================================================

Công việc chỉ được xem là hoàn thành khi:

- Giao diện bám visual design của Admin Dashboard.
- Banner đồng nhất với Notification.
- List/table/card đồng nhất với “Tất cả thông báo”.
- Button đồng nhất.
- Pill đồng nhất.
- Modal đồng nhất.
- Validation đồng nhất với form “Gửi đề xuất mới”.
- Toast đồng nhất với toast thêm/bỏ yêu thích.
- Không còn chuỗi mới viết cứng.
- English và Vietnamese hoạt động.
- Không mất code của bất kỳ thành viên nào.
- Không thay đổi nghiệp vụ ngoài phạm vi.
- Không làm hỏng filter, pagination, form hoặc JavaScript.
- Compile thành công.
- Test liên quan thành công.
- `git diff --check` không có lỗi.
- Có báo cáo đầy đủ các file đã sửa và chức năng được giữ nguyên.
