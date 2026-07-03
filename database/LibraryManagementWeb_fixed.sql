USE [master]
GO
/****** Object:  Database [tes]    Script Date: 7/3/2026 5:56:34 PM ******/
CREATE DATABASE [tes]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'tes', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\tes.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'tes_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\tes_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [tes] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [tes].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [tes] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [tes] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [tes] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [tes] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [tes] SET ARITHABORT OFF 
GO
ALTER DATABASE [tes] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [tes] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [tes] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [tes] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [tes] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [tes] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [tes] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [tes] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [tes] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [tes] SET  ENABLE_BROKER 
GO
ALTER DATABASE [tes] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [tes] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [tes] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [tes] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [tes] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [tes] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [tes] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [tes] SET RECOVERY FULL 
GO
ALTER DATABASE [tes] SET  MULTI_USER 
GO
ALTER DATABASE [tes] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [tes] SET DB_CHAINING OFF 
GO
ALTER DATABASE [tes] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [tes] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [tes] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [tes] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
EXEC sys.sp_db_vardecimal_storage_format N'tes', N'ON'
GO
ALTER DATABASE [tes] SET QUERY_STORE = ON
GO
ALTER DATABASE [tes] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [tes]
GO
/****** Object:  Table [dbo].[Account_Roles]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Accounts]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Authors]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookAcquisitionRequests]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookAuthors]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookDisposals]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookItems]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Books]    Script Date: 7/3/2026 5:56:34 PM ******/
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
	[image] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[book_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BorrowDetails]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Borrows]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Categories]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Favorites]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Feedbacks]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Genres]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MemberNotifications]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Members]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MembershipTiers]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Notifications]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PasswordResetTokens]    Script Date: 7/3/2026 5:56:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PasswordResetTokens](
	[reset_token_id] [int] IDENTITY(1,1) NOT NULL,
	[token] [varchar](36) NOT NULL,
	[user_id] [int] NOT NULL,
	[expiry_date] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[reset_token_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Reservations]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Roles]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Shelves]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Staff]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemLogs]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemSettings]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Transactions]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users]    Script Date: 7/3/2026 5:56:34 PM ******/
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
	[avatar] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Wallets]    Script Date: 7/3/2026 5:56:34 PM ******/
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
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
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
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (21, 3)
INSERT [dbo].[Account_Roles] ([account_id], [role_id]) VALUES (22, 3)
GO
SET IDENTITY_INSERT [dbo].[Accounts] ON 

INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (1, 1, N'admin', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve.', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (2, 2, N'librarian01', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve.', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (3, 3, N'librarian02', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (4, 4, N'member01', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (5, 5, N'member02', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (6, 6, N'member03', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (7, 7, N'member04', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (8, 8, N'member05', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (9, 9, N'member06', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (10, 10, N'member07', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (11, 11, N'member08', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (12, 12, N'member09', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (13, 13, N'member10', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (14, 14, N'member11', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (15, 15, N'member12', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (16, 16, N'quocanh', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (17, 17, N'wusenminu', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (18, 18, N'sa', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (19, 19, N'nhoc2323@gmail.com', N'', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (20, 20, N'test1', N'$2a$10$OiRgm5HFnPF55ivgfoWAyuwykjkh5lKVhaWwK92bPmEjL2QrE2p5a', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (21, 21, N'test2', N'$2a$10$v00NLAXLiK2NZaA2DqHOU.t.kZmASSI9kIhVrPBQAfxPFq9mIJB4q', N'Active')
INSERT [dbo].[Accounts] ([account_id], [user_id], [username], [password_hash], [status]) VALUES (22, 22, N'thuongnt.ce191329@gmail.com', N'', N'Active')
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

INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (1, 1, N'Chí Phèo', N'9786040100003', N'Truyện ngắn kinh điển Việt Nam.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (2, 3, N'Clean Code', N'9786040100014', N'Sách gối đầu giường của lập trình viên.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (3, 5, N'Sapiens: Lược Sử Loài Người', N'9786040100010', N'Lịch sử tiến hóa nhân loại.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (4, 11, N'Harry Potter và Hòn Đá Phù Thủy', N'9786040100007', N'Tiểu thuyết phép thuật nổi tiếng.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (5, 10, N'Đắc Nhân Tâm', N'9786040100021', N'Nghệ thuật thu phục lòng người.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (6, 1, N'Nhà Giả Kim', N'9786040100038', N'Hành trình đi tìm kho báu.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (7, 12, N'Phía Sau Nghi Can X', N'9786040100045', N'Trinh thám Nhật Bản hấp dẫn.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (8, 1, N'Mắt Biếc', N'9786040100052', N'Tình yêu tuổi học trò buồn.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (9, 7, N'Người Giàu Có Nhất Thành Babylon', N'9786040100069', N'Bí quyết làm giàu từ xa xưa.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (10, 2, N'Dế Mèn Phiêu Lưu Ký', N'9786040100076', N'Truyện thiếu nhi kinh điển.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (11, 3, N'Clean Architecture', N'9786040100083', N'Kiến trúc phần mềm sạch.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (12, 5, N'21 Bài Học Cho Thế Kỷ 21', N'9786040100090', N'Góc nhìn sâu sắc về hiện tại.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (13, 11, N'Harry Potter và Phòng Chứa Bí Mật', N'9786040100106', N'Tập 2 của Harry Potter.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (14, 1, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', N'9786040100113', N'Ký ức tuổi thơ tươi đẹp.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (15, 12, N'Bạch Dạ Hành', N'9786040100120', N'Tiểu thuyết trinh thám ám ảnh.', N'Active', NULL)
SET IDENTITY_INSERT [dbo].[Books] OFF
GO
SET IDENTITY_INSERT [dbo].[BorrowDetails] ON 

INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (1, 1, 1, 2, CAST(N'2026-06-28T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (2, 2, 3, 6, CAST(N'2026-06-18T20:01:08.297' AS DateTime), NULL, 0, N'Overdue')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (3, 3, 7, 15, CAST(N'2026-07-03T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (4, 4, 9, 19, CAST(N'2026-07-06T20:01:08.297' AS DateTime), NULL, 0, N'Borrowed')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (5, 5, 5, 10, CAST(N'2026-06-08T20:01:08.297' AS DateTime), CAST(N'2026-06-06T20:01:08.297' AS DateTime), 0, N'Returned')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (6, 6, 6, 13, CAST(N'2026-06-23T20:01:08.297' AS DateTime), NULL, 0, N'Overdue')
INSERT [dbo].[BorrowDetails] ([borrow_detail_id], [borrow_id], [book_id], [book_item_id], [due_date], [return_date], [renew_count], [status]) VALUES (7, 7, 5, NULL, CAST(N'2026-07-15T22:48:08.387' AS DateTime), NULL, 0, N'Pending')
SET IDENTITY_INSERT [dbo].[BorrowDetails] OFF
GO
SET IDENTITY_INSERT [dbo].[Borrows] ON 

INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (1, 1, 2, CAST(N'2026-06-14T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (2, 3, 3, CAST(N'2026-06-04T20:01:08.297' AS DateTime), N'Overdue')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (3, 5, 2, CAST(N'2026-06-19T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (4, 7, 3, CAST(N'2026-06-22T20:01:08.297' AS DateTime), N'Active')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (5, 2, 2, CAST(N'2026-05-25T20:01:08.297' AS DateTime), N'Returned')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (6, 9, 3, CAST(N'2026-06-09T20:01:08.297' AS DateTime), N'Overdue')
INSERT [dbo].[Borrows] ([borrow_id], [member_id], [staff_id], [borrow_date], [status]) VALUES (7, 17, NULL, CAST(N'2026-07-01T22:48:08.383' AS DateTime), N'Pending')
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
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (16, 12)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (16, 15)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (17, 14)
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
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (11, 16, 5, 4, N'll', CAST(N'2026-06-30T16:07:09.857' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (12, 17, 4, 5, N'égrsdh', CAST(N'2026-07-01T22:48:25.477' AS DateTime), N'APPROVED', N'nguuu', CAST(N'2026-07-01T22:48:47.277' AS DateTime))
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
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (17, 14, 1, CAST(N'2026-07-01T22:49:01.047' AS DateTime))
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
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (16, 19, NULL)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (17, 20, NULL)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (18, 21, 1)
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (19, 22, 1)
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
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (14, NULL, N'Phản hồi đánh giá', N'Thủ thư đã phản hồi đánh giá của bạn cho sách ''Harry Potter và Hòn Đá Phù Thủy''.', CAST(N'2026-07-01T22:48:47.280' AS DateTime), N'Active')
SET IDENTITY_INSERT [dbo].[Notifications] OFF
GO
SET IDENTITY_INSERT [dbo].[PasswordResetTokens] ON 

INSERT [dbo].[PasswordResetTokens] ([reset_token_id], [token], [user_id], [expiry_date]) VALUES (1, N'a608ceb6-c42b-47b1-9b9f-ef9e4c6cc708', 20, CAST(N'2026-07-03T13:11:11.6583185' AS DateTime2))
SET IDENTITY_INSERT [dbo].[PasswordResetTokens] OFF
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
SET IDENTITY_INSERT [dbo].[SystemLogs] ON 

INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (1, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 5D0E0E199EE2C20E8F7546CD549961E8', CAST(N'2026-06-30T16:06:39.070' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (2, 19, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 01C89BC503E9CE169B12DF04F3C8D1DE', CAST(N'2026-06-30T16:08:16.140' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (3, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 01C89BC503E9CE169B12DF04F3C8D1DE', CAST(N'2026-06-30T16:08:56.320' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (4, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 2B4E36BA44D37874373EBD7A12E5778B', CAST(N'2026-06-30T16:08:56.537' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (5, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: A3199CBD16166D1A0E2917BFDB3700D6', CAST(N'2026-06-30T16:11:10.587' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (6, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 0168C839C9CD28D6DD057C0F08DEA1C2', CAST(N'2026-07-01T22:06:58.617' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (7, 19, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 9D493B32BA6C73E50C6B4282B3073AB8', CAST(N'2026-07-01T22:07:01.237' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (8, 1, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 9D493B32BA6C73E50C6B4282B3073AB8', CAST(N'2026-07-01T22:32:23.117' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (9, 1, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 8E849DA2A13BACBA883822926F86520A', CAST(N'2026-07-01T22:32:27.090' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (10, 1, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 8E849DA2A13BACBA883822926F86520A', CAST(N'2026-07-01T22:38:29.997' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (11, 1, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: E8E990AA2AE94D818B4F102F18F82556', CAST(N'2026-07-01T22:39:12.237' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (12, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: E8E990AA2AE94D818B4F102F18F82556', CAST(N'2026-07-01T22:39:21.440' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (13, 1, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 375F3B28E1AE9D20B020EF8413B691DA', CAST(N'2026-07-01T22:39:45.947' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (14, 1, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: A8620FFE9CE99B755D935E1D23040865', CAST(N'2026-07-01T22:39:47.947' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (15, 2, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: A8620FFE9CE99B755D935E1D23040865', CAST(N'2026-07-01T22:40:32.753' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (16, 2, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 77C272BE4883684A3ECDF89747A71B4D', CAST(N'2026-07-01T22:40:49.990' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (17, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 77C272BE4883684A3ECDF89747A71B4D', CAST(N'2026-07-01T22:40:52.263' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (18, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 5466FCE4A70B89BF314ABA3206F269FE', CAST(N'2026-07-01T22:41:10.947' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (19, 19, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 75CE20589A15B8D353735879C9184467', CAST(N'2026-07-01T22:41:19.677' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (20, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 75CE20589A15B8D353735879C9184467', CAST(N'2026-07-01T22:43:11.160' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (21, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: D27B2E914A58A1C0808BFDBED6C03A1F', CAST(N'2026-07-01T22:47:56.613' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (22, 20, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 7EFA7EF66180DB4932D3AA93F0E403BC', CAST(N'2026-07-01T22:48:28.237' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (23, 2, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 7EFA7EF66180DB4932D3AA93F0E403BC', CAST(N'2026-07-01T22:48:37.480' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (24, 2, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 2F632D94E77D2B554C32921EAF6D0E02', CAST(N'2026-07-01T22:48:49.090' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (25, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: 2F632D94E77D2B554C32921EAF6D0E02', CAST(N'2026-07-01T22:48:54.477' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (26, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: F436DB2583189B7D5A36C50EBCB948E4', CAST(N'2026-07-01T22:51:07.293' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (27, 19, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: AD43F3FCB936828E44BB183FF98F775A', CAST(N'2026-07-01T23:18:19.973' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (28, 19, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: CC162781615C2936E7198A21B865CCCF', CAST(N'2026-07-01T23:19:09.510' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (29, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: CC162781615C2936E7198A21B865CCCF', CAST(N'2026-07-01T23:21:06.647' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (30, 20, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: DB03C859CCECBBD2D1989C71D8FC9F9D', CAST(N'2026-07-01T23:21:34.253' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (31, 20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng nhập thành công. Session ID: DB03C859CCECBBD2D1989C71D8FC9F9D', CAST(N'2026-07-01T23:21:39.010' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [account_id], [action_type], [ip_address], [user_agent], [description], [created_at]) VALUES (32, 20, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Đăng xuất thành công. Session ID: 3AADEBE80089AD041886FA17CB80299F', CAST(N'2026-07-01T23:21:56.890' AS DateTime))
INSERT [dbo].[SystemLogs] ([log_id], [accou