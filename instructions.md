# AI Agent Instructions for LMW UI Work

## 0. Mục tiêu

Bạn đang chỉnh giao diện của một dự án **Spring Boot + Thymeleaf + Bootstrap** đã có chức năng. Nhiệm vụ mặc định là thay đổi presentation, không thiết kế lại nghiệp vụ.

Hai file bắt buộc phải đọc trước khi làm UI:

1. `instructions.md` — quy trình làm việc và giới hạn thay đổi.
2. `DESIGN-claude.md` — visual contract và số đo canonical.

Quy tắc áp dụng cho mọi AI coding agent. Không phụ thuộc Codex, Claude, Antigravity, Cursor hay IDE cụ thể.

## 1. Nguyên tắc ưu tiên

Khi thông tin xung đột, dùng thứ tự sau:

1. Yêu cầu mới nhất của người dùng và ảnh tham chiếu được chỉ định là đúng.
2. Behavior hiện có: route, controller, binding, validation, quyền và JavaScript hook.
3. Hợp đồng định lượng trong `DESIGN-claude.md`.
4. Component tương đương đã hoàn thiện trong repository.
5. Bootstrap defaults hoặc style legacy.

Nếu ảnh tham chiếu chỉ yêu cầu sửa một defect, không được thay đổi các phần khác dù tài liệu có gợi ý một cách trình bày khác.

## 2. Phạm vi mặc định

Được phép khi task yêu cầu chỉnh UI:

- HTML/Thymeleaf structure và semantic wrappers;
- CSS của đúng page family;
- class Bootstrap, icon, accessibility attributes;
- JavaScript chỉ phục vụ interaction/presentation;
- responsive, loading, empty, modal, toast và visual validation state.

Không được tự ý sửa:

- controller, service, repository, entity, DTO, mapper;
- security, database, SQL, API, route;
- authentication/authorization/session;
- validation, payment, email, borrow/return business rules;
- dependency, config, test hoặc generated output;
- layout/shared component không thuộc scope.

Chỉ mở rộng sang file thứ ba khi template + stylesheet owner không đủ để hoàn thành yêu cầu. Phải nêu chính xác dependency trước khi sửa.

## 3. Quy trình bắt buộc

### Phase A — Hiểu yêu cầu

Trích xuất rõ:

- màn hình/URL cần sửa;
- role và state cần hiển thị;
- defect hoặc outcome cụ thể;
- ảnh/reference nào là chuẩn;
- phần nào phải giữ nguyên;
- task là chỉnh một phần hay chuẩn hóa cả page.

Không đoán thêm feature từ một mockup. Không thêm KPI, filter, nút hoặc dữ liệu chưa tồn tại chỉ để lấp layout.

### Phase B — Inspect repository trước khi code

Phải dùng repository search (`rg`, IDE search hoặc tương đương), không chỉ nhìn open tabs. Xác định tối thiểu:

1. URL và controller mapping.
2. Template thực sự được controller return hoặc layout section thực sự sở hữu màn hình.
3. Layout đang decorate và thứ tự stylesheet được load.
4. CSS owner đang được template link.
5. Fragment/JS liên quan.
6. Component/trang tương đương của cùng hoặc role khác.
7. Mọi behavior hook: `id`, `name`, `th:*`, `data-*`, `aria-*`, form action/method, modal/tab target.
8. CSS cascade xung đột, inline style, `<style>`, `!important` hoặc selector trùng.

Không tạo file vì không thấy nó trong editor. Chỉ tạo sau khi search xác nhận không có owner phù hợp.

### Phase C — Lập scope manifest nội bộ

Trước khi edit, ghi lại:

```text
Task outcome:
Target URL:
Controller mapping:
Controller-returned template/section:
Active layout:
Page root class:
Canonical CSS owner:
Reference component/page:
Behavior hooks to preserve:
Allowed files:
Excluded files:
New page required: yes/no + reason:
New CSS required: yes/no + reason:
```

Mặc định một page task chỉ sửa tối đa hai implementation files:

1. template thực sự render;
2. stylesheet owner của page family.

Hai file tài liệu này chỉ được sửa khi người dùng yêu cầu sửa quy tắc.

### Phase D — Chọn owner, không tạo duplicate

CSS-owner decision tree:

1. Dùng stylesheet page-family đã link trong target template.
2. Nếu chưa có, dùng shared family stylesheet của component tương đương đã chuẩn hóa.
3. Nếu cả hai không có, tạo đúng một file `src/main/resources/static/css/pages/{role}/{feature}.css` và link trong target template.

File mới phải mở đầu:

```css
/*
 * Page family: <tên nghiệp vụ>
 * Templates: templates/<role>/<page>.html
 * Routes: <route>
 * Root scope: .<feature>-page
 */
```

Không tạo `new`, `v2`, `copy`, `custom`, `fix`, `temp`, `style`, `styles`, `common` hoặc `global` để né owner hiện có. Không tạo một trang mới cho POST/PUT/DELETE. Feature cùng workflow phải mở rộng primary page hiện có bằng section/card/modal/tab/fragment hợp lý.

### Phase E — Implement tối thiểu

- Copy hierarchy/classes của canonical component; không chỉ bắt chước màu.
- Scope page rules dưới root class.
- Dùng `--app-*` tokens có sẵn.
- Giữ diff surgical: không reformat toàn file, đổi line endings, sort CSS hoặc cleanup ngoài scope.
- Presentation nằm trong CSS owner, không thêm inline style hoặc `<style>`.
- Dùng semantic class cho state; finite state dùng `th:classappend`, không `th:style`.
- Giữ một visible frame và một overflow owner.
- Không sửa font-size để chữa wrap/overflow.
- Không đổi text/wording trừ khi task yêu cầu hoặc sửa lỗi hiển nhiên thuộc scope.

## 4. Behavior lock — phải giữ nguyên

Không rename, remove hoặc thay đổi ý nghĩa của:

- `th:each`, `th:if`, `th:unless`, `th:text`, `th:value`, `th:field`;
- `th:href`, `th:src`, `th:action`, `th:object`, `th:replace`, `th:insert`, `th:fragment`;
- form `action`, `method`, `name`, `id`, hidden inputs và CSRF;
- field names, validation attributes, option/radio/checkbox values;
- query parameters, pagination/filter/sort parameters;
- IDs/classes/data attributes được JavaScript, Bootstrap, labels, tests hoặc deep-link dùng;
- `data-bs-*`, modal/tab/collapse ownership, `aria-controls`, `aria-expanded`;
- permission/role conditions, status mapping và model attribute.

Được thêm wrapper/class khi không phá DOM relationship. Nếu buộc phải chuyển một behavior hook, cập nhật mọi consumer trong cùng scope và test lại.

## 5. Thymeleaf và form safety

- HTML vẫn phải hợp lệ sau khi thêm điều kiện/loop.
- Không lồng `<form>`.
- Button phải có `type` rõ ràng.
- Label phải trỏ đúng field ID.
- Submit của form tách rời dùng thuộc tính `form="..."` và giữ CSRF/hidden fields trong owning form.
- Không convert server-side validation thành client-only validation.
- Không thay giá trị backend bằng text hard-code để khớp mockup.
- Không thêm fake data vào production template. Placeholder chỉ nằm trong fallback text của element có `th:text` nếu cần.

## 6. Quy tắc làm theo ảnh tham chiếu

Ảnh là visual evidence, không phải bằng chứng về business behavior.

1. Dùng đúng một ảnh/page được người dùng khóa làm reference chính.
2. So sánh cùng viewport, zoom 100%, sidebar state và data state.
3. Tách khác biệt thành: structure, typography, spacing, color, component state, responsive.
4. Nếu user chỉ nêu một lỗi, chỉ sửa lỗi đó.
5. Không merge sở thích từ nhiều ảnh thành một thiết kế mới.
6. Không dùng zoom, sidebar collapse hoặc `overflow:hidden` để che lỗi width.

Nếu không có ảnh, dùng `DESIGN-claude.md` + component tương đương trong repo làm chuẩn.

## 7. Kiểm tra trong khi implement

### Structure

- đúng target template, không phải duplicate/unused page;
- đúng layout và stylesheet load order;
- page root class tồn tại;
- cùng DOM hierarchy/direct-child order với canonical component;
- không nested frame, nested form hoặc duplicate ID.

### Visual

- computed font, size, weight, line-height;
- foreground/background/border/icon/focus colors;
- width, height, padding, gap, radius, shadow;
- action order/alignment và table column width;
- overflow owner và long Vietnamese content.

Một CSS rule đúng trong source nhưng thua cascade vẫn là lỗi. Tìm owner/xung đột thay vì tăng specificity tùy ý.

### States

Kiểm tra những state áp dụng được:

- default, hover, focus-visible, active;
- disabled, readonly;
- valid, invalid, server error, success;
- selected/current/unread;
- loading;
- empty data và no-results;
- text/name/title/email rất dài;
- số lượng row action thực tế.

### Responsive

Kiểm tra tối thiểu:

| Viewport | Zoom | Sidebar |
|---|---:|---|
| 1440×900 | 100% | expanded |
| 1024×768 | 100% | expanded |
| 768×1024 | 100% | canonical behavior |
| 390×844 | 100% | collapsed/overlay |

Từ 1024px không có horizontal scrollbar cho dữ liệu thông thường. Chỉ table wrapper canonical được cuộn ngang khi thực sự cần ở màn nhỏ.

## 8. Verification theo mức rủi ro

Sau khi edit:

1. Đọc lại diff, không chỉ file cuối cùng.
2. Chạy `git status --short`; mọi file thay đổi phải nằm trong manifest. Workspace có thể đã dirty: không revert thay đổi của người dùng.
3. Search target template cho `<style`, `style=`, `th:style`, `element.style`, duplicate selector và raw color mới.
4. Search mọi reference tới ID/class/hook đã di chuyển.
5. Chạy test/build phù hợp với dự án nếu khả dụng. Với Maven, ưu tiên test hẹp trước rồi mới test rộng nếu cần.
6. Nếu có thể render, kiểm tra console, network stylesheet, Thymeleaf error và computed style.
7. Hard refresh để tránh duyệt CSS cache cũ.

Không tuyên bố đã kiểm tra browser nếu chưa thực sự render. Hãy ghi rõ phần nào kiểm tra bằng source/build và phần nào chưa thể kiểm tra runtime.

## 9. Các lỗi agent thường mắc — đều bị cấm

- Sửa template có tên giống route nhưng controller không return template đó.
- Tạo page/CSS mới thay vì tiếp tục owner hiện có.
- Redesign toàn trang khi user chỉ yêu cầu căn một nút hoặc sửa một component.
- Dùng Bootstrap blue, raw hex, gradient hoặc shadow tùy hứng.
- Dùng card lồng card và nhiều lớp border/radius.
- Đưa CSS vào template cho nhanh.
- Xóa `th:*`, hidden field, CSRF hoặc JS hook trong lúc sắp xếp DOM.
- Đổi backend/business logic để dễ render.
- Dùng `!important`, absolute position, transform hoặc `nth-child` như layout hack.
- Thu nhỏ chữ, browser zoom hoặc hide overflow để che tràn.
- Sửa shared layout cho một page mà không đánh giá mọi consumer.
- Báo “responsive” sau khi chỉ nhìn desktop.
- Báo “pixel-perfect” khi chưa kiểm tra computed styles/runtime.

## 10. Completion gate

Không kết thúc task nếu một mục áp dụng được còn fail:

- [ ] Đúng URL/controller/template/section.
- [ ] Tái sử dụng page và CSS owner hiện có.
- [ ] Không tạo duplicate page/component/stylesheet.
- [ ] Mọi behavior hook và Thymeleaf binding được giữ.
- [ ] Design tokens và canonical measurements đúng.
- [ ] Một visible frame và một overflow owner.
- [ ] Default + interaction + validation + empty + long-content state đúng.
- [ ] Bốn viewport không có lỗi layout nghiêm trọng.
- [ ] Keyboard focus và accessible name đầy đủ.
- [ ] Không có business logic hoặc file ngoài scope bị sửa.
- [ ] Diff nhỏ, không có reformat/cleanup ngoài yêu cầu.
- [ ] Build/test phù hợp đã pass, hoặc limitation được nói rõ.

## 11. Format báo cáo bắt buộc

Trả lời ngắn, dựa trên bằng chứng:

```text
Kết quả:
- <outcome người dùng nhận được>

Files đã cập nhật:
- <path>: <trách nhiệm thay đổi>

Đã tái sử dụng:
- Page/template: <path>
- CSS/component owner: <path>

Đã tạo mới:
- none | <path + lý do repository-backed>

Validation:
- <test/build/source/runtime check và kết quả>
- functionality/Thymeleaf hooks preserved
- no duplicate page or CSS owner
- no unrelated files modified by this task

Chưa kiểm tra được:
- none | <runtime limitation cụ thể>
```

Không nói chung chung “đã tối ưu UI”. Nêu component nào thay đổi và bằng chứng nào đã kiểm tra.

## 12. Khi chưa chắc chắn

Ưu tiên bảo toàn behavior và diff nhỏ. Tiếp tục inspect repository trước khi hỏi. Chỉ hỏi người dùng khi lựa chọn còn thiếu sẽ thay đổi đáng kể outcome, ví dụ không biết ảnh nào là chuẩn hoặc task có thể là sửa một defect so với redesign toàn trang.
