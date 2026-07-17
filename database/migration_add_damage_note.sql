-- ================================================
-- Migration Script: Thêm cột damage_note vào BookItems
-- Tác dụng: Ghi nhận ghi chú hư hỏng khi tiếp nhận sách hoàn trả
-- Ngày tạo: 2026-07-15
-- ================================================

-- Thêm cột damage_note vào bảng BookItems (nếu chưa tồn tại)
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'BookItems' AND COLUMN_NAME = 'damage_note'
)
BEGIN
    ALTER TABLE [dbo].[BookItems]
    ADD [damage_note] NVARCHAR(255) NULL;
    PRINT 'Đã thêm cột damage_note vào bảng BookItems thành công.';
END
ELSE
BEGIN
    PRINT 'Cột damage_note đã tồn tại trong bảng BookItems, bỏ qua.';
END
GO
