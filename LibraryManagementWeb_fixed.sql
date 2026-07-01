USE [master]
GO

IF DB_ID(N'LibraryManagementWeb') IS NOT NULL
BEGIN
    ALTER DATABASE [LibraryManagementWeb] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [LibraryManagementWeb];
END
GO

CREATE DATABASE [LibraryManagementWeb];
GO

USE [LibraryManagementWeb];
GO

/****** Object:  Table [dbo].[Account_Roles]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Account_Roles](
	[account_id] [int] NOT NULL,
	[role_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[account_id] ASC,
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Accounts]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Accounts](
	[account_id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NULL,
	[username] [varchar](100) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[account_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Authors]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Authors](
	[author_id] [int] IDENTITY(1,1) NOT NULL,
	[author_name] [nvarchar](255) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[author_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookAcquisitionRequests]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookAcquisitionRequests](
	[request_id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NOT NULL,
	[title] [nvarchar](255) NOT NULL,
	[created_date] [datetime] NULL,
	[author] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[request_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookAuthors]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookAuthors](
	[book_id] [int] NOT NULL,
	[author_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[book_id] ASC,
	[author_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookDisposals]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookDisposals](
	[disposal_id] [int] IDENTITY(1,1) NOT NULL,
	[book_item_id] [int] NULL,
	[staff_id] [int] NULL,
	[reason] [nvarchar](max) NULL,
	[disposal_date] [datetime] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[disposal_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookItems]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookItems](
	[book_item_id] [int] IDENTITY(1,1) NOT NULL,
	[book_id] [int] NULL,
	[shelf_id] [int] NULL,
	[barcode] [varchar](50) NOT NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[book_item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Books]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Books](
	[book_id] [int] IDENTITY(1,1) NOT NULL,
	[genre_id] [int] NULL,
	[title] [nvarchar](255) NOT NULL,
	[isbn] [varchar](20) NULL,
	[description] [nvarchar](max) NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[book_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BorrowDetails]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BorrowDetails](
	[borrow_detail_id] [int] IDENTITY(1,1) NOT NULL,
	[borrow_id] [int] NULL,
	[book_id] [int] NULL,
	[book_item_id] [int] NULL,
	[due_date] [datetime] NOT NULL,
	[return_date] [datetime] NULL,
	[renew_count] [int] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[borrow_detail_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Borrows]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Borrows](
	[borrow_id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NULL,
	[staff_id] [int] NULL,
	[borrow_date] [datetime] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[borrow_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Categories]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Categories](
	[category_id] [int] IDENTITY(1,1) NOT NULL,
	[category_name] [nvarchar](255) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[category_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Favorites]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Favorites](
	[member_id] [int] NOT NULL,
	[book_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[member_id] ASC,
	[book_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Feedbacks]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Feedbacks](
	[feedback_id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NULL,
	[book_id] [int] NULL,
	[rating] [int] NULL,
	[comment] [nvarchar](max) NULL,
	[created_date] [datetime] NULL,
	[status] [varchar](50) NULL,
	[librarian_response] [nvarchar](max) NULL,
	[response_date] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[feedback_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Genres]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Genres](
	[genre_id] [int] IDENTITY(1,1) NOT NULL,
	[category_id] [int] NULL,
	[genre_name] [nvarchar](255) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[genre_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MemberNotifications]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MemberNotifications](
	[member_id] [int] NOT NULL,
	[notification_id] [int] NOT NULL,
	[is_read] [bit] NULL,
	[read_date] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[member_id] ASC,
	[notification_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Members]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Members](
	[member_id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NULL,
	[tier_id] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[member_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MembershipTiers]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MembershipTiers](
	[tier_id] [int] IDENTITY(1,1) NOT NULL,
	[tier_name] [nvarchar](100) NOT NULL,
	[discount_percent] [decimal](5, 2) NULL,
	[borrow_limit] [int] NULL,
	[condition] [decimal](18, 2) NULL,
	[benefits] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[tier_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Notifications]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Notifications](
	[notification_id] [int] IDENTITY(1,1) NOT NULL,
	[staff_id] [int] NULL,
	[title] [nvarchar](255) NOT NULL,
	[content] [nvarchar](max) NOT NULL,
	[created_date] [datetime] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[notification_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Reservations]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Reservations](
	[reservation_id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NULL,
	[book_id] [int] NULL,
	[reservation_date] [datetime] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[reservation_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Roles]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Roles](
	[role_id] [int] IDENTITY(1,1) NOT NULL,
	[name] [varchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Shelves]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Shelves](
	[shelf_id] [int] IDENTITY(1,1) NOT NULL,
	[shelf_name] [nvarchar](100) NOT NULL,
	[location] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[shelf_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Staff]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Staff](
	[staff_id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NULL,
	[staff_type] [varchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[staff_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemLogs]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SystemLogs](
	[log_id] [int] IDENTITY(1,1) NOT NULL,
	[account_id] [int] NULL,
	[action_type] [varchar](100) NOT NULL,
	[ip_address] [varchar](50) NULL,
	[user_agent] [nvarchar](max) NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[log_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemSettings]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SystemSettings](
	[setting_id] [int] IDENTITY(1,1) NOT NULL,
	[setting_key] [varchar](100) NOT NULL,
	[setting_value] [nvarchar](max) NULL,
	[description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[setting_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Transactions]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Transactions](
	[transaction_id] [int] IDENTITY(1,1) NOT NULL,
	[wallet_id] [int] NULL,
	[borrow_id] [int] NULL,
	[transaction_type] [varchar](50) NOT NULL,
	[amount] [decimal](18, 2) NOT NULL,
	[transaction_date] [datetime] NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[transaction_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Users](
	[user_id] [int] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](255) NOT NULL,
	[email] [varchar](255) NOT NULL,
	[phone] [varchar](20) NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Wallets]    Script Date: 28/06/2026 11:36:46 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Wallets](
	[wallet_id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NULL,
	[balance] [decimal](18, 2) NULL,
PRIMARY KEY CLUSTERED 
(
	[wallet_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (1, 1)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (2, 2)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (3, 2)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (4, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (5, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (6, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (7, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (8, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (9, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (10, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (11, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (12, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (13, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (14, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (15, 3)
GO
SET IDENTITY_INSERT [dbo].[Accounts] ON 

INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (1, 1, N'admin', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (2, 2, N'librarian01', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (3, 3, N'librarian02', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (4, 4, N'member01', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (5, 5, N'member02', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (6, 6, N'member03', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (7, 7, N'member04', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (8, 8, N'member05', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (9, 9, N'member06', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (10, 10, N'member07', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (11, 11, N'member08', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (12, 12, N'member09', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (13, 13, N'member10', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (14, 14, N'member11', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (15, 15, N'member12', N'$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (16, 16, N'quocanh', N'$2a$10$IuVZoed54AL1jy4eKPK3j.MjYK0BUej2yhQ655PJQxYvr4ozidHfm', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (17, 17, N'wusenminu', N'$2a$10$1mdevMAKYn07DVa091ID4.5LsatFj2b3YxUgeZW6xbP/DJIEp8seu', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (18, 18, N'sa', N'$2a$10$9g1GXlJffmuFyCpLvYSIReEWDz92PUFVHi.tgav9Z7eB35FEvcmgy', N'Active')
SET IDENTITY_INSERT [dbo].[Accounts] OFF
GO
SET IDENTITY_INSERT [dbo].[Authors] ON 

INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (1, N'Nam Cao')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (2, N'Robert C. Martin')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (3, N'Yuval Noah Harari')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (4, N'J.K. Rowling')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (5, N'Dale Carnegie')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (6, N'Paulo Coelho')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (7, N'Higashino Keigo')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (8, N'Nguyễn Nhật Ánh')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (9, N'George S. Clason')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (10, N'Tô Hoài')
SET IDENTITY_INSERT [dbo].[Authors] OFF
GO
SET IDENTITY_INSERT [dbo].[BookAcquisitionRequests] ON 

INSERT [dbo].[BookAcquisitionRequests] ([request_id], [member_id], [title], [created_date], [author]) VALUES (1, 13, N'Ngồi khóc trên cây', CAST(N'2026-06-28T18:52:49.760' AS DateTime), NULL)
INSERT [dbo].[BookAcquisitionRequests] ([request_id], [member_id], [title], [created_date], [author]) VALUES (2, 13, N'Cô gái đến từ hôm qua', CAST(N'2026-06-28T19:00:58.883' AS DateTime), N'Nguyễn Nhật Ánh')
SET IDENTITY_INSERT [dbo].[BookAcquisitionRequests] OFF
GO
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (1, 1)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (2, 2)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (3, 3)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (4, 4)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (5, 5)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (6, 6)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (7, 7)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (8, 8)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (9, 9)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (10, 10)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (11, 2)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (12, 3)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (13, 4)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (14, 8)
INSERT [dbo].[BookAuthors] ([book_id], [author_id]) VALUES (15, 7)
GO
SET IDENTITY_INSERT [dbo].[BookItems] ON 

INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (1, 1, 1, N'BC001-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (2, 1, 1, N'BC001-002', N'Borrowed')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (3, 2, 3, N'BC002-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (4, 2, 3, N'BC002-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (5, 2, 3, N'BC002-003', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (6, 3, 2, N'BC003-001', N'Borrowed')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (7, 3, 2, N'BC003-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (8, 4, 2, N'BC004-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (9, 4, 2, N'BC004-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (10, 5, 4, N'BC005-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (11, 5, 4, N'BC005-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (12, 5, 4, N'BC005-003', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (13, 6, 2, N'BC006-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (14, 6, 2, N'BC006-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (15, 7, 5, N'BC007-001', N'Borrowed')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (16, 7, 5, N'BC007-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (17, 8, 1, N'BC008-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (18, 8, 1, N'BC008-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (19, 9, 4, N'BC009-001', N'Borrowed')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (20, 9, 4, N'BC009-002', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (21, 10, 1, N'BC010-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (22, 11, 3, N'BC011-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (23, 12, 2, N'BC012-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (24, 13, 2, N'BC013-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (25, 14, 1, N'BC014-001', N'Available')
INSERT [dbo].[BookItems] ([book_item_id], [book_id], [shelf_id], [barcode], [status]) VALUES (26, 15, 5, N'BC015-001', N'Available')
SET IDENTITY_INSERT [dbo].[BookItems] OFF
GO
SET IDENTITY_INSERT [dbo].[Books] ON 

INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (1, 1, N'Chí Phèo', N'9786040100003', N'Truyện ngắn kinh điển Việt Nam.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (2, 3, N'Clean Code', N'9786040100014', N'Sách gối đầu giường của lập trình viên.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (3, 5, N'Sapiens: Lược Sử Loài Người', N'9786040100010', N'Lịch sử tiến hóa nhân loại.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (4, 11, N'Harry Potter và Hòn Đá Phù Thủy', N'9786040100007', N'Tiểu thuyết phép thuật nổi tiếng.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (5, 10, N'Đắc Nhân Tâm', N'9786040100021', N'Nghệ thuật thu phục lòng người.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (6, 1, N'Nhà Giả Kim', N'9786040100038', N'Hành trình đi tìm kho báu.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (7, 12, N'Phía Sau Nghi Can X', N'9786040100045', N'Trinh thám Nhật Bản hấp dẫn.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (8, 1, N'Mắt Biếc', N'9786040100052', N'Tình yêu tuổi học trò buồn.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (9, 7, N'Người Giàu Có Nhất Thành Babylon', N'9786040100069', N'Bí quyết làm giàu từ xa xưa.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (10, 2, N'Dế Mèn Phiêu Lưu Ký', N'9786040100076', N'Truyện thiếu nhi kinh điển.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (11, 3, N'Clean Architecture', N'9786040100083', N'Kiến trúc phần mềm sạch.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (12, 5, N'21 Bài Học Cho Thế Kỷ 21', N'9786040100090', N'Góc nhìn sâu sắc về hiện tại.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (13, 11, N'Harry Potter và Phòng Chứa Bí Mật', N'9786040100106', N'Tập 2 của Harry Potter.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (14, 1, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', N'9786040100113', N'Ký ức tuổi thơ tươi đẹp.', N'Active')
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status]) VALUES (15, 12, N'Bạch Dạ Hành', N'9786040100120', N'Tiểu thuyết trinh thám ám ảnh.', N'Active')
SET IDENTITY_INSERT [dbo].[Books] OFF
GO
SET IDENTITY_INSERT [dbo].[BorrowDetails] ON 

INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (1, 1, 1, 2, CAST(N'2026-06-28T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (2, 2, 3, 6, CAST(N'2026-06-18T20:01:08.297' AS DateTime), NULL, 0, N'Overdue')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (3, 3, 7, 15, CAST(N'2026-07-03T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (4, 4, 9, 19, CAST(N'2026-07-06T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (5, 5, 5, 10, CAST(N'2026-06-08T20:01:08.297' AS DateTime), CAST(N'2026-06-06T20:01:08.297' AS DateTime), 0, N'Returned')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (6, 6, 6, 13, CAST(N'2026-06-23T20:01:08.297' AS DateTime), NULL, 0, N'Overdue')
SET IDENTITY_INSERT [dbo].[BorrowDetails] OFF
GO
SET IDENTITY_INSERT [dbo].[Borrows] ON 

INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (1, 1, 2, CAST(N'2026-06-14T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (2, 3, 3, CAST(N'2026-06-04T20:01:08.297' AS DateTime), N'Overdue')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (3, 5, 2, CAST(N'2026-06-19T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (4, 7, 3, CAST(N'2026-06-22T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (5, 2, 2, CAST(N'2026-05-25T20:01:08.297' AS DateTime), N'Returned')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (6, 9, 3, CAST(N'2026-06-09T20:01:08.297' AS DateTime), N'Overdue')
SET IDENTITY_INSERT [dbo].[Borrows] OFF
GO
SET IDENTITY_INSERT [dbo].[Categories] ON 

INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (1, N'Văn học')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (2, N'Khoa học - Công nghệ')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (3, N'Lịch sử - Địa lý')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (4, N'Kinh tế - Tài chính')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (5, N'Kỹ năng sống')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (6, N'Tiểu thuyết - Hư cấu')
SET IDENTITY_INSERT [dbo].[Categories] OFF
GO
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (1, 2)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (1, 4)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (1, 7)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (2, 5)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (2, 8)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (3, 3)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (3, 11)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (4, 1)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (5, 9)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (6, 7)
GO
SET IDENTITY_INSERT [dbo].[Feedbacks] ON 

INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (1, 1, 1, 5, N'Tác phẩm kinh điển, rất ý nghĩa.', CAST(N'2026-06-19T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (2, 2, 5, 5, N'Đọc xong thấy bản thân thay đổi nhiều.', CAST(N'2026-06-22T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (3, 4, 2, 4, N'Khá hay nhưng hơi khó đọc cho người mới.', CAST(N'2026-06-23T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (4, 6, 7, 5, N'Cốt truyện quá đỉnh, không đoán được cái kết.', CAST(N'2026-06-24T20:01:08.300' AS DateTime), N'APPROVED', N'Cảm ơn', CAST(N'2026-06-28T20:23:27.203' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (5, 8, 10, 5, N'Sách gắn liền với tuổi thơ.', CAST(N'2026-06-24T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (6, 4, 1, 5, N'Sách rất hay, kiến thức bổ ích!', CAST(N'2026-06-24T21:13:47.710' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (7, 13, 1, 3, N'Hay', CAST(N'2026-06-28T01:24:17.643' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (8, 13, 3, 1, N'Quá dở', CAST(N'2026-06-28T01:50:03.073' AS DateTime), N'APPROVED', N'Ok hehe', CAST(N'2026-06-28T20:23:10.113' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (9, 13, 6, 4, N'Hay lắm', CAST(N'2026-06-28T01:50:17.470' AS DateTime), N'APPROVED', N'Ok', CAST(N'2026-06-28T20:12:18.697' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (10, 13, 12, 5, N'Hay quá đi', CAST(N'2026-06-28T17:43:36.710' AS DateTime), N'APPROVED', N'Ok cảm ơn', CAST(N'2026-06-28T17:47:04.447' AS DateTime))
SET IDENTITY_INSERT [dbo].[Feedbacks] OFF
GO
SET IDENTITY_INSERT [dbo].[Genres] ON 

INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (1, 1, N'Tiểu thuyết văn học')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (2, 1, N'Truyện ngắn')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (3, 2, N'Công nghệ thông tin')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (4, 2, N'Khoa học vũ trụ')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (5, 3, N'Lịch sử thế giới')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (6, 3, N'Lịch sử Việt Nam')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (7, 4, N'Đầu tư tài chính')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (8, 4, N'Kinh doanh khởi nghiệp')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (9, 5, N'Tâm lý học')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (10, 5, N'Phát triển bản thân')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (11, 6, N'Hành động - Kỳ ảo')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (12, 6, N'Trinh thám')
SET IDENTITY_INSERT [dbo].[Genres] OFF
GO
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (1, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (1, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (1, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (2, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (2, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (2, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (3, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (3, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (3, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (4, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (4, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (4, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (5, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (5, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (5, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (6, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (6, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (6, 11, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (6, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (7, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (7, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (7, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (8, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (8, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (8, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (9, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (9, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (9, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (10, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (10, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (10, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (11, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (11, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (11, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (12, 2, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (12, 3, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (12, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 8, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 9, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 10, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 12, 0, NULL)
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 13, 0, NULL)
GO
SET IDENTITY_INSERT [dbo].[Members] ON 

INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (1, 4, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (2, 5, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (3, 6, 2)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (4, 7, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (5, 8, 3)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (6, 9, 4)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (7, 10, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (8, 11, 2)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (9, 12, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (10, 13, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (11, 14, 2)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (12, 15, 3)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (13, 16, NULL)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (14, 17, NULL)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (15, 18, NULL)
SET IDENTITY_INSERT [dbo].[Members] OFF
GO
SET IDENTITY_INSERT [dbo].[MembershipTiers] ON 

INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (1, N'Regular', CAST(0.00 AS Decimal(5, 2)), 3, CAST(0.00 AS Decimal(18, 2)), N'Mượn tối đa 3 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (2, N'Silver', CAST(10.00 AS Decimal(5, 2)), 5, CAST(500000.00 AS Decimal(18, 2)), N'Giảm 10% phí mượn, mượn tối đa 5 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (3, N'Gold', CAST(20.00 AS Decimal(5, 2)), 10, CAST(1000000.00 AS Decimal(18, 2)), N'Giảm 20% phí mượn, mượn tối đa 10 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (4, N'Diamond', CAST(50.00 AS Decimal(5, 2)), 20, CAST(5000000.00 AS Decimal(18, 2)), N'Giảm 50% phí mượn, mượn tối đa 20 sách')
SET IDENTITY_INSERT [dbo].[MembershipTiers] OFF
GO
SET IDENTITY_INSERT [dbo].[Notifications] ON 

INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (2, NULL, N'Test Thông báo', N'thông báo tới toàn member', CAST(N'2026-06-27T21:15:04.470' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (3, NULL, N'abc', N'123', CAST(N'2026-06-27T21:33:16.973' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (8, NULL, N'Phản hồi đánh giá', N'Thủ thư đã phản hồi đánh giá của bạn cho sách ''21 Bài Học Cho Thế Kỷ 21''.', CAST(N'2026-06-28T17:47:04.453' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (9, NULL, N'Phản hồi đánh giá', N'Thủ thư đã phản hồi đánh giá của bạn cho sách ''Nhà Giả Kim''.', CAST(N'2026-06-28T20:12:18.707' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (10, NULL, N'Phản hồi đánh giá', N'Thủ thư đã phản hồi đánh giá của bạn cho sách ''Sapiens: Lược Sử Loài Người''.', CAST(N'2026-06-28T20:23:10.123' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (11, NULL, N'Phản hồi đánh giá', N'Thủ thư đã phản hồi đánh giá của bạn cho sách ''Phía Sau Nghi Can X''.', CAST(N'2026-06-28T20:23:27.203' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (12, NULL, N'abc', N'abc', CAST(N'2026-06-28T20:23:55.437' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (13, NULL, N'M ngu', N'abc', CAST(N'2026-06-28T21:13:36.080' AS DateTime), N'Active')
SET IDENTITY_INSERT [dbo].[Notifications] OFF
GO
SET IDENTITY_INSERT [dbo].[Roles] ON 

INSERT [dbo].[Roles] ([role_id], [name]) VALUES (1, N'ADMIN')
INSERT [dbo].[Roles] ([role_id], [name]) VALUES (2, N'LIBRARIAN')
INSERT [dbo].[Roles] ([role_id], [name]) VALUES (3, N'MEMBER')
SET IDENTITY_INSERT [dbo].[Roles] OFF
GO
SET IDENTITY_INSERT [dbo].[Shelves] ON 

INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (1, N'Kệ A1 - Văn học VN', N'Tầng 1 - Khu A')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (2, N'Kệ A2 - Văn học NN', N'Tầng 1 - Khu A')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (3, N'Kệ B1 - IT', N'Tầng 2 - Khu B')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (4, N'Kệ C1 - Kỹ năng', N'Tầng 3 - Khu C')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (5, N'Kệ D1 - Trinh thám', N'Tầng 2 - Khu D')
SET IDENTITY_INSERT [dbo].[Shelves] OFF
GO
SET IDENTITY_INSERT [dbo].[Staff] ON 

INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (1, 1, N'Admin')
INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (2, 2, N'Librarian')
INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (3, 3, N'Librarian')
SET IDENTITY_INSERT [dbo].[Staff] OFF
GO
SET IDENTITY_INSERT [dbo].[SystemSettings] ON 

INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (1, N'Fine_Per_Day', N'5000', N'Phí phạt trễ hạn tính theo ngày (VND)')
INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (2, N'Max_Borrow_Days', N'14', N'Số ngày mượn sách tối đa tiêu chuẩn')
INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (3, N'Deposit_Amount', N'50000', N'Tiền cọc đặt trước một quyển sách (VND)')
SET IDENTITY_INSERT [dbo].[SystemSettings] OFF
GO
SET IDENTITY_INSERT [dbo].[Transactions] ON 

INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (1, 1, NULL, N'TOP_UP', CAST(200000.00 AS Decimal(18, 2)), CAST(N'2026-06-09T20:01:08.297' AS DateTime), N'Completed')
INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (2, 1, 1, N'BORROW_FEE', CAST(-5000.00 AS Decimal(18, 2)), CAST(N'2026-06-14T20:01:08.297' AS DateTime), N'Completed')
INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (3, 3, NULL, N'TOP_UP', CAST(500000.00 AS Decimal(18, 2)), CAST(N'2026-05-25T20:01:08.297' AS DateTime), N'Completed')
INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (4, 3, 2, N'BORROW_FEE', CAST(-2500.00 AS Decimal(18, 2)), CAST(N'2026-06-04T20:01:08.297' AS DateTime), N'Completed')
INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (5, 5, NULL, N'TOP_UP', CAST(1000000.00 AS Decimal(18, 2)), CAST(N'2026-06-14T20:01:08.297' AS DateTime), N'Completed')
INSERT [dbo].[Transactions] ([transaction_id], [wallet_id], [borrow_id], [transaction_type], [amount], [transaction_date], [status]) VALUES (6, 2, 5, N'BORROW_FEE', CAST(-5000.00 AS Decimal(18, 2)), CAST(N'2026-05-25T20:01:08.297' AS DateTime), N'Completed')
SET IDENTITY_INSERT [dbo].[Transactions] OFF
GO
SET IDENTITY_INSERT [dbo].[Users] ON 

INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (1, N'Admin Tổng', N'admin@library.vn', N'0901000001', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (2, N'Nguyễn Thị Lan', N'lib01@library.vn', N'', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (3, N'Trần Văn Minh', N'lib02@library.vn', N'0902000002', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (4, N'Lê Thị Hoa', N'member01@gmail.com', N'0903000001', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (5, N'Phạm Minh Tuấn', N'member02@gmail.com', N'0903000002', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (6, N'Nguyễn Văn An', N'member03@gmail.com', N'0903000003', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (7, N'Trần Thị Mai', N'member04@gmail.com', N'0903000004', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (8, N'Hoàng Văn Dũng', N'member05@gmail.com', N'0903000005', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (9, N'Đặng Thu Hà', N'member06@gmail.com', N'0903000006', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (10, N'Vũ Đức Cường', N'member07@gmail.com', N'0903000007', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (11, N'Bùi Thị Thanh', N'member08@gmail.com', N'0903000008', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (12, N'Đỗ Xuân Hùng', N'member09@gmail.com', N'0903000009', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (13, N'Ngô Bảo Châu', N'member10@gmail.com', N'0903000010', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (14, N'Lý Hải Yến', N'member11@gmail.com', N'0903000011', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (15, N'Dương Tấn Sang', N'member12@gmail.com', N'0903000012', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (16, N'Quốc Anh', N'tnquocanh.ce191655@gmail.com', N'0856989555', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (17, N'Thế Hiển', N'quocanh26032005@gmail.com', N'0856989555', N'Active')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status]) VALUES (18, N'uuuwuwuuw', N'dantnl.ce191088@gmail.com', N'08569895553', N'Active')
SET IDENTITY_INSERT [dbo].[Users] OFF
GO
SET IDENTITY_INSERT [dbo].[Wallets] ON 

INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (1, 1, CAST(150000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (2, 2, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (3, 3, CAST(300000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (4, 4, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (5, 5, CAST(500000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (6, 6, CAST(1200000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (7, 7, CAST(10000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (8, 8, CAST(250000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (9, 9, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (10, 10, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (11, 11, CAST(15000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (12, 12, CAST(1000000.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (13, 13, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (14, 14, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (15, 15, CAST(0.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[Wallets] OFF
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Accounts__F3DBC57227C9EBBB]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[Accounts] ADD UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__BookItem__C16E36F8B409C9E8]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[BookItems] ADD UNIQUE NONCLUSTERED 
(
	[barcode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Books__99F9D0A4C834C099]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[Books] ADD UNIQUE NONCLUSTERED 
(
	[isbn] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Roles__72E12F1BB3A159DE]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[Roles] ADD UNIQUE NONCLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__SystemSe__0DFAC4271D0D9AD0]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[SystemSettings] ADD UNIQUE NONCLUSTERED 
(
	[setting_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Users__AB6E6164B2487540]    Script Date: 28/06/2026 11:36:46 pm ******/
ALTER TABLE [dbo].[Users] ADD UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Accounts] ADD  DEFAULT ('Active') FOR [status]
GO
ALTER TABLE [dbo].[BookAcquisitionRequests] ADD  DEFAULT (getdate()) FOR [created_date]
GO
ALTER TABLE [dbo].[BookDisposals] ADD  DEFAULT (getdate()) FOR [disposal_date]
GO
ALTER TABLE [dbo].[BookDisposals] ADD  DEFAULT ('Completed') FOR [status]
GO
ALTER TABLE [dbo].[BookItems] ADD  DEFAULT ('Available') FOR [status]
GO
ALTER TABLE [dbo].[Books] ADD  DEFAULT ('Active') FOR [status]
GO
ALTER TABLE [dbo].[BorrowDetails] ADD  DEFAULT ((0)) FOR [renew_count]
GO
ALTER TABLE [dbo].[BorrowDetails] ADD  DEFAULT ('Borrowed') FOR [status]
GO
ALTER TABLE [dbo].[Borrows] ADD  DEFAULT (getdate()) FOR [borrow_date]
GO
ALTER TABLE [dbo].[Borrows] ADD  DEFAULT ('Active') FOR [status]
GO
ALTER TABLE [dbo].[Feedbacks] ADD  DEFAULT (getdate()) FOR [created_date]
GO
ALTER TABLE [dbo].[Feedbacks] ADD  DEFAULT ('PENDING') FOR [status]
GO
ALTER TABLE [dbo].[MemberNotifications] ADD  DEFAULT ((0)) FOR [is_read]
GO
ALTER TABLE [dbo].[MembershipTiers] ADD  DEFAULT ((0)) FOR [discount_percent]
GO
ALTER TABLE [dbo].[MembershipTiers] ADD  DEFAULT ((5)) FOR [borrow_limit]
GO
ALTER TABLE [dbo].[MembershipTiers] ADD  DEFAULT ((0)) FOR [condition]
GO
ALTER TABLE [dbo].[Notifications] ADD  DEFAULT (getdate()) FOR [created_date]
GO
ALTER TABLE [dbo].[Notifications] ADD  DEFAULT ('Active') FOR [status]
GO
ALTER TABLE [dbo].[Reservations] ADD  DEFAULT (getdate()) FOR [reservation_date]
GO
ALTER TABLE [dbo].[Reservations] ADD  DEFAULT ('Pending') FOR [status]
GO
ALTER TABLE [dbo].[SystemLogs] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[Transactions] ADD  DEFAULT (getdate()) FOR [transaction_date]
GO
ALTER TABLE [dbo].[Transactions] ADD  DEFAULT ('Completed') FOR [status]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ('Active') FOR [status]
GO
ALTER TABLE [dbo].[Wallets] ADD  DEFAULT ((0)) FOR [balance]
GO
ALTER TABLE [dbo].[Account_Roles]  WITH CHECK ADD FOREIGN KEY([account_id])
REFERENCES [dbo].[Accounts] ([account_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Account_Roles]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[Roles] ([role_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Accounts]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[Users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BookAcquisitionRequests]  WITH CHECK ADD  CONSTRAINT [FK_BookAcquisitionRequests_Member] FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
GO
ALTER TABLE [dbo].[BookAcquisitionRequests] CHECK CONSTRAINT [FK_BookAcquisitionRequests_Member]
GO
ALTER TABLE [dbo].[BookAuthors]  WITH CHECK ADD FOREIGN KEY([author_id])
REFERENCES [dbo].[Authors] ([author_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BookAuthors]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BookDisposals]  WITH CHECK ADD FOREIGN KEY([book_item_id])
REFERENCES [dbo].[BookItems] ([book_item_id])
GO
ALTER TABLE [dbo].[BookDisposals]  WITH CHECK ADD FOREIGN KEY([staff_id])
REFERENCES [dbo].[Staff] ([staff_id])
GO
ALTER TABLE [dbo].[BookItems]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BookItems]  WITH CHECK ADD FOREIGN KEY([shelf_id])
REFERENCES [dbo].[Shelves] ([shelf_id])
GO
ALTER TABLE [dbo].[Books]  WITH CHECK ADD FOREIGN KEY([genre_id])
REFERENCES [dbo].[Genres] ([genre_id])
GO
ALTER TABLE [dbo].[BorrowDetails]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
GO
ALTER TABLE [dbo].[BorrowDetails]  WITH CHECK ADD FOREIGN KEY([book_item_id])
REFERENCES [dbo].[BookItems] ([book_item_id])
GO
ALTER TABLE [dbo].[BorrowDetails]  WITH CHECK ADD FOREIGN KEY([borrow_id])
REFERENCES [dbo].[Borrows] ([borrow_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Borrows]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
GO
ALTER TABLE [dbo].[Borrows]  WITH CHECK ADD FOREIGN KEY([staff_id])
REFERENCES [dbo].[Staff] ([staff_id])
GO
ALTER TABLE [dbo].[Favorites]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Favorites]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Feedbacks]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Feedbacks]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Genres]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[Categories] ([category_id])
GO
ALTER TABLE [dbo].[MemberNotifications]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[MemberNotifications]  WITH CHECK ADD FOREIGN KEY([notification_id])
REFERENCES [dbo].[Notifications] ([notification_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Members]  WITH CHECK ADD FOREIGN KEY([tier_id])
REFERENCES [dbo].[MembershipTiers] ([tier_id])
GO
ALTER TABLE [dbo].[Members]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[Users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Notifications]  WITH CHECK ADD FOREIGN KEY([staff_id])
REFERENCES [dbo].[Staff] ([staff_id])
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([book_id])
REFERENCES [dbo].[Books] ([book_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Staff]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[Users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[SystemLogs]  WITH CHECK ADD FOREIGN KEY([account_id])
REFERENCES [dbo].[Accounts] ([account_id])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[Transactions]  WITH CHECK ADD FOREIGN KEY([borrow_id])
REFERENCES [dbo].[Borrows] ([borrow_id])
GO
ALTER TABLE [dbo].[Transactions]  WITH CHECK ADD FOREIGN KEY([wallet_id])
REFERENCES [dbo].[Wallets] ([wallet_id])
GO
ALTER TABLE [dbo].[Wallets]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Feedbacks]  WITH CHECK ADD CHECK  (([rating]>=(1) AND [rating]<=(5)))
GO
GO
