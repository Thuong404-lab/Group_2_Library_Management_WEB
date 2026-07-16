# LMW UI Design Contract

> Tên file được giữ để tương thích với quy trình hiện tại. Đây là đặc tả giao diện cho **Library Management Web (LMW)**, không phải yêu cầu sao chép website Claude/Anthropic.

## 1. Cách đọc tài liệu này

Tài liệu này là nguồn sự thật duy nhất về hình thức giao diện. `instructions.md` là nguồn sự thật về cách agent làm việc. Khi có xung đột, ưu tiên theo thứ tự:

1. Yêu cầu mới nhất và ảnh tham chiếu do người dùng cung cấp.
2. Hợp đồng định lượng và cấu trúc canonical trong file này.
3. Component tương đương đã hoàn thiện trong repository.
4. Bootstrap mặc định và giao diện cũ chưa được chuẩn hóa.

Các từ **MUST / PHẢI**, **MUST NOT / KHÔNG ĐƯỢC** là tiêu chí pass/fail. Không tự sáng tạo biến thể nếu đã có cấu trúc hoặc số đo canonical.

## 2. Định hướng thị giác

LMW là ứng dụng quản lý thư viện có cảm giác ấm, điềm tĩnh và mang tính biên tập:

- nền kem ấm, không dùng trắng lạnh hoặc xám xanh kiểu dashboard SaaS;
- tiêu đề serif thanh lịch, nội dung và control dùng sans-serif dễ đọc;
- màu nâu là màu hành động chính trong ứng dụng;
- coral chỉ dùng cho focus, liên kết hoặc điểm nhấn nhẹ;
- card phẳng, viền mảnh, gần như không đổ bóng;
- mật độ vừa phải: rõ ràng, nhiều khoảng thở nhưng không giống landing page;
- trạng thái dùng màu semantic nhạt, luôn có chữ hoặc icon đi kèm;
- Admin và Librarian dùng cùng ngôn ngữ component, không có palette riêng theo role.

Không sử dụng gradient, glassmorphism, neon, shadow dày, góc bo quá lớn, card lồng card, hero marketing khổng lồ hoặc trang trí không phục vụ nghiệp vụ.

## 3. Design tokens bắt buộc

Khi layout đã cung cấp biến `--app-*`, CSS trang PHẢI dùng biến đó. Không sao chép hex vào page CSS và không tạo alias mới cho cùng ý nghĩa.

### 3.1 Màu

| Vai trò | CSS token | Giá trị chuẩn |
|---|---|---:|
| Accent coral | `--app-color-primary` | `#cc785c` |
| Accent coral active | `--app-color-primary-active` | `#a9583e` |
| Tiêu đề/ink | `--app-color-ink` | `#141413` |
| Nội dung | `--app-color-body` | `#3d3d3a` |
| Nội dung nhấn | `--app-color-body-strong` | `#252523` |
| Chữ phụ | `--app-color-muted` | `#6c6a64` |
| Chữ phụ nhẹ | `--app-color-muted-soft` | `#8e8b82` |
| Viền | `--app-color-hairline` | `#e6dfd8` |
| Viền nhẹ | `--app-color-hairline-soft` | `#ebe6df` |
| Canvas/card chính | `--app-color-canvas` | `#faf9f5` |
| Vùng phụ/hover | `--app-color-surface-soft` | `#f5f0e8` |
| Card phụ/selected | `--app-color-surface-card` | `#efe9de` |
| CTA ứng dụng | `--app-color-application-primary` | `#8b5a2b` |
| CTA hover | `--app-color-application-primary-hover` | `#6b4423` |
| CTA active | `--app-color-application-primary-active` | `#58371d` |
| Chữ trên nền tối | `--app-color-on-dark` | `#faf9f5` |

Semantic mapping cố định:

| Ý nghĩa | Nền | Viền | Chữ/icon |
|---|---|---|---|
| Thành công/đã hoàn tất/có sẵn | `--app-color-success-soft` | `--app-color-success-border` | `--app-color-success-ink` |
| Chờ/chú ý | `--app-color-warning-soft` | `--app-color-warning-border` | `--app-color-warning-ink` |
| Lỗi/nguy hiểm/từ chối/quá hạn | `--app-color-danger-soft` | `--app-color-danger-border` | `--app-color-danger-ink` |
| Trung tính | `--app-color-surface-card` | `--app-color-hairline` | `--app-color-body` |

Màu semantic không dùng để trang trí category hoặc role. Không dùng các class màu Bootstrap như `bg-primary`, `btn-primary`, `text-info` nếu computed color khác mapping trên.

### 3.2 Typography

- Display: `var(--app-font-display)`; fallback thực tế hiện tại là Playfair Display.
- UI: `var(--app-font-ui)`; fallback sans-serif hệ thống.
- `h1` của trang: `clamp(34px, 4vw, 48px)`, weight 500, line-height 1.1.
- Eyebrow: 12px/600, uppercase, letter-spacing `.12em`, màu muted.
- Mô tả trang: 15px/400, line-height 1.5, màu muted.
- Section title: 21–24px/500, display font.
- Card title: 16–18px/600.
- Body/control/table: 14px/400, line-height 1.4–1.55.
- Label: 13–14px/600.
- Helper/caption: 12–13px/400–500.
- Button: 14px/600.

Chỉ `h1` được scale bằng `clamp()`. Không thu nhỏ chữ để chữa overflow. Sửa grid, width owner hoặc wrapping.

### 3.3 Shape, spacing và motion

- Radius control: `var(--app-radius-md)` = 8px.
- Radius card/modal: `var(--app-radius-lg)` = 12px.
- Radius badge: `var(--app-radius-pill/full)` = 9999px.
- Spacing scale: 4, 8, 12, 16, 24, 32, 48px.
- Content-area do layout sở hữu; page không cộng thêm một lớp padding lớn trùng lặp.
- Motion: 150–200ms, chỉ cho color, opacity và transform nhỏ.
- Với `prefers-reduced-motion: reduce`, bỏ animation/transform không thiết yếu.

## 4. Khung trang canonical

Mọi trang nghiệp vụ mới hoặc trang được chuẩn hóa PHẢI có một root class riêng, ví dụ `.admin-accounts-page`, `.notification-compose-page`.

```html
<main layout:fragment="content" class="feature-page">
  <header class="feature-header">
    <div class="feature-header-copy">
      <span class="feature-eyebrow">NHÓM NGHIỆP VỤ</span>
      <h1>Tiêu đề trang</h1>
      <p>Một câu mô tả ngắn, hữu ích.</p>
    </div>
    <div class="feature-header-actions">...</div>
  </header>

  <section class="feature-toolbar">...</section>
  <section class="app-card">...</section>
</main>
```

Header desktop: flex, `align-items:flex-end`, `justify-content:space-between`, gap 24px, padding-bottom 24px, margin-bottom 24–26px, border-bottom 1px hairline. Mobile dưới 768px: xếp dọc, action full width khi hợp lý.

Không thêm eyebrow, KPI, summary hoặc CTA chỉ để trang “đẹp hơn” nếu chúng không có dữ liệu/nghiệp vụ thật. Giữ wording trong yêu cầu hoặc màn hình tham chiếu.

## 5. Component canonical

### 5.1 Button

Primary application action:

```css
height: 40px;
padding: 0 18px;
border-radius: 8px;
background: var(--app-color-application-primary);
color: var(--app-color-on-dark);
font: 600 14px/1 var(--app-font-ui);
```

- Hover dùng `application-primary-hover`; active dùng `application-primary-active`.
- Focus-visible dùng ring 3px từ token focus của layout.
- Nút phụ: canvas + hairline + ink; hover surface-soft.
- Icon action: đúng 36×36px, hình tròn, icon giữa, có `title` và `aria-label`.
- Delete action dùng danger ink, không dùng solid red mặc định.
- CTA tạo/thêm/gửi/lưu: text button. Row action xem/sửa/xóa: icon button.
- Một cụm action có thứ tự ổn định: primary/confirm trước, secondary/cancel sau; row action xem → sửa → xóa.

### 5.2 Form

- Input/select height 42px, padding ngang 14px, border 1px hairline, radius 8px, canvas.
- Textarea padding 12px 14px, resize vertical.
- Label cách control 7–8px.
- Field gap 16px; group/section gap 24px.
- Focus: border coral + ring 3px; không chỉ đổi màu mà không có focus indicator.
- Invalid: danger border/soft ring và thông báo chữ; valid chỉ hiển thị khi thực sự cần.
- Placeholder 14px/400 màu muted.
- Không thay `name`, `id`, `th:field`, constraint, hidden input hoặc CSRF.

Action row có text action và icon action:

```html
<div class="app-form-actions">
  <button class="app-form-primary">Lưu</button>
  <button class="app-icon-action" aria-label="Xóa" title="Xóa">...</button>
</div>
```

Row là flex không wrap, gap 8px; primary co giãn, icon giữ 36px. Không lồng form. Nếu icon submit form khác, dùng thuộc tính HTML `form="id-cua-form"` và form chủ sở hữu ẩn `.app-detached-form`.

### 5.3 Card và frame

`.app-card`: canvas, border 1px hairline, radius 12px, không shadow; padding thường 20–24px.

Một data region chỉ có **một visible frame**. Nếu card sở hữu border/radius thì table wrapper bên trong không có border/radius. Không lồng `.app-card`, `.card`, `.table-responsive` chỉ để tạo thêm khung.

### 5.4 Table

- Width 100%, border-collapse collapse, font 14px.
- Header: surface-soft, chữ muted 12px/600, padding 12px 16px.
- Cell: padding 14px 16px, border-bottom hairline, vertical-align middle.
- Primary cell: ink, weight 500–600.
- Chỉ `.app-table-wrap` được `overflow-x:auto`.
- Parent grid/flex phải `min-width:0`; wrapper `width/max-width:100%` và `box-sizing:border-box`.
- Desktop từ 1024px không được có horizontal scrollbar với dữ liệu thông thường.

Action column dùng `<colgroup>` và một width owner duy nhất:

- một action: 72px;
- hai action: 88px;
- ba action: 132px.

Header và mọi cell action cùng class, text-align center, padding ngang 0. `.app-row-actions` dùng flex center, gap 8px. Không dùng `nth-child`, transform hoặc margin hack để căn icon.

### 5.5 Badge/status

Badge: inline-flex, center, padding 5px 11px, pill, 12px/600, nowrap, border 1px.

Loan status mapping cố định:

| Status | Nhãn | Semantic |
|---|---|---|
| `PENDING` | Chờ duyệt | warning |
| `RENEW_PENDING` | Chờ gia hạn | warning |
| `RETURN_PENDING` | Xin trả sách | warning |
| `BORROWED` | Đang mượn | neutral |
| `OVERDUE` | Quá hạn | danger |
| `RETURNED` | Đã hoàn trả | success |
| `CANCELED` | Đã hủy | danger |
| `REJECTED` | Đã từ chối | danger |
| null/unknown | Chưa rõ | neutral muted |

Backend value phải trim + uppercase trước mapping nếu template hiện tại cần mapping. Không đổi business status.

### 5.6 Toolbar, search và filter

- Toolbar cùng một visual row trên desktop, gap 12px, wrap có chủ đích.
- Search chiếm phần còn lại, filter có width hữu hạn, CTA đứng cuối.
- Mọi control cao 42px và căn cùng baseline.
- Mobile: xếp dọc, width 100%; không thu font hoặc dùng horizontal scrolling cho form control.
- Search icon nằm trong control hoặc ngay trước input theo component tham chiếu; không trộn hai kiểu trên các trang tương đương.

### 5.7 Modal, toast, dropdown

- Một backdrop, một modal frame và một scroll owner.
- Modal: canvas, border hairline, radius 12px, shadow overlay token, clip góc ở shell.
- Header/body/footer có padding nhất quán 20–24px; footer action căn phải, mobile có thể full width.
- Toast tối đa 360px, nội dung 14px; có icon + chữ, không chỉ màu.
- Tooltip nền tối, chữ sáng, ngắn gọn.
- Không đặt z-index tùy ý nếu Bootstrap/layout đã sở hữu stacking.

### 5.8 Empty/loading/error state

Empty state đặt trong data region, min-height 112–160px, icon 22–28px muted-soft, tiêu đề + hướng dẫn ngắn. No-results khác empty-data nếu nghiệp vụ cần phân biệt. Loading không làm layout nhảy kích thước. Server alert dùng semantic mapping và nội dung hiện có.

## 6. Hợp đồng riêng đã khóa

### 6.1 Notification compose

Màn Librarian “Gửi thông báo” dùng root `.notification-compose-page` và stylesheet owner hiện có `static/css/pages/librarian/send-notification.css`.

- Card ngoài: canvas, hairline, radius 12px, no shadow.
- Body: 24px desktop, 16px mobile.
- Grid desktop: `minmax(280px,.85fr) minmax(0,1.15fr)`, gap 32px.
- Trái là đối tượng nhận; phải là tiêu đề, nội dung và submit.
- Hai cột cùng background, không divider, không card con full-height.
- Choice group: surface-soft, hairline, radius 8px, padding 16px.
- Member list max-height 260px, overflow-y auto, padding 16px.
- Textarea min-height 196px.
- Submit 40px, text-only, căn phải; mobile full width.
- Dưới 992px chuyển đúng một cột.

Không thêm icon gửi, tiêu đề trang trí riêng cho từng cột, gradient hoặc shadow.

### 6.2 Surgical screenshot fix

Khi người dùng nói ảnh/màn hình hiện tại đúng ngoại trừ một lỗi cụ thể, khóa toàn bộ phần không được nêu: màu, typography, spacing, badge, filter, cột và wording. Chỉ sửa defect được chỉ ra. Không “đồng bộ hóa” các phần khác trong cùng task.

## 7. Responsive và accessibility

Kiểm tra tối thiểu ở 1440×900, 1024×768, 768×1024 và 390×844, zoom 100%.

- Breakpoint chính: dưới 992px giảm grid; dưới 768px stack header/toolbar/action phù hợp.
- Tap target tối thiểu 40×40px; icon action canonical 36px được chấp nhận khi có khoảng cách rõ.
- Có keyboard focus nhìn thấy được.
- Label liên kết đúng `for/id`; icon trang trí có `aria-hidden="true"`.
- Icon-only action có `aria-label` và `title`.
- Modal phải trả focus sau khi đóng; tab/accordion cập nhật `aria-selected/expanded`.
- Text normal contrast tối thiểu 4.5:1; large text và component boundary 3:1.
- Nội dung tiếng Việt dài phải wrap; không cắt mất dữ liệu quan trọng.

## 8. Những điều tuyệt đối không làm

- Không tạo trang `-new`, `-v2`, `-copy` hoặc CSS `custom.css`, `fix.css`, `style.css` khi owner đã tồn tại.
- Không thêm raw hex, gradient, Bootstrap blue hoặc palette riêng theo role vào component đã chuẩn hóa.
- Không dùng inline `style`, `<style>`, `th:style` cho presentation hữu hạn; dùng semantic class trong stylesheet owner.
- Không giải quyết cascade bằng `!important` trừ khi phải thắng một legacy `!important` đã xác minh và không thể loại bỏ an toàn.
- Không đổi layout/sidebar/header/footer/global CSS cho một page task nếu không thật sự cần.
- Không thay đổi nghiệp vụ, route, Thymeleaf binding hoặc JavaScript hook để đạt giao diện.
- Không tuyên bố “giống thiết kế” chỉ dựa trên source CSS; phải xét computed result và viewport.

## 9. Definition of Done về thiết kế

Một UI chỉ đạt khi:

1. đúng template thực sự được controller trả về;
2. dùng đúng layout và một stylesheet owner canonical;
3. cùng component structure với trang tương đương;
4. màu, type, spacing, radius, control và action đúng token/số đo;
5. một frame và một overflow owner cho mỗi data region;
6. đủ state default, hover, focus, disabled, invalid, empty và long-content;
7. không overflow desktop và usable trên bốn viewport;
8. không làm mất binding, behavior hoặc accessibility hook;
9. diff chỉ chứa thay đổi thuộc phạm vi yêu cầu;
10. nếu có ảnh tham chiếu, phần không được yêu cầu sửa vẫn giữ nguyên.
