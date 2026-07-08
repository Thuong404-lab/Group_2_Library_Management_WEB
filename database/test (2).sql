USE [master]
GO
/****** Object:  Database [tes]    Script Date: 03/07/2026 11:44:19 pm ******/
IF DB_ID(N'tes') IS NULL
BEGIN
    CREATE DATABASE [tes]
END
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
/****** Object:  Table [dbo].[Authors]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[BookAcquisitionRequests]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[BookAuthors]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[BookDisposals]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[BookItems]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Books]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[BorrowDetails]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Borrows]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Categories]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Favorites]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Feedbacks]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Genres]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Member_Account_Roles]    Script Date: 03/07/2026 11:44:19 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Member_Account_Roles](
	[member_account_id] [int] NOT NULL,
	[role_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[member_account_id] ASC,
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Member_Accounts]    Script Date: 03/07/2026 11:44:19 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Member_Accounts](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[member_id] [int] NOT NULL,
	[username] [varchar](100) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MemberNotifications]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Members]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[MembershipTiers]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Notifications]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[PasswordResetTokens]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Reservations]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Roles]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Shelves]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Staff]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Staff_Account_Roles]    Script Date: 03/07/2026 11:44:19 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Staff_Account_Roles](
	[staff_account_id] [int] NOT NULL,
	[role_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[staff_account_id] ASC,
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Staff_Accounts]    Script Date: 03/07/2026 11:44:19 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Staff_Accounts](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[staff_id] [int] NOT NULL,
	[username] [varchar](100) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[status] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemLogs]    Script Date: 03/07/2026 11:44:19 pm ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SystemLogs](
	[log_id] [int] IDENTITY(1,1) NOT NULL,
	[action_type] [varchar](100) NOT NULL,
	[ip_address] [varchar](50) NULL,
	[user_agent] [nvarchar](max) NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
	[user_id] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[log_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemSettings]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Transactions]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Users]    Script Date: 03/07/2026 11:44:19 pm ******/
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
/****** Object:  Table [dbo].[Wallets]    Script Date: 03/07/2026 11:44:19 pm ******/
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
SET IDENTITY_INSERT [dbo].[Authors] ON 

INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (1, N'Nam Cao')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (2, N'Robert C. Martin')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (3, N'Yuval Noah Harari')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (4, N'J.K. Rowling')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (5, N'Dale Carnegie')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (6, N'Paulo Coelho')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (7, N'Higashino Keigo')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (8, N'Nguy?n Nh?t Ánh')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (9, N'George S. Clason')
INSERT [dbo].[Authors] ([author_id], [author_name]) VALUES (10, N'Tô Hoài')
SET IDENTITY_INSERT [dbo].[Authors] OFF
GO
SET IDENTITY_INSERT [dbo].[BookAcquisitionRequests] ON 

INSERT [dbo].[BookAcquisitionRequests] ([request_id], [member_id], [title], [created_date], [author]) VALUES (1, 13, N'Ng?i khóc trên cây', CAST(N'2026-06-28T18:52:49.760' AS DateTime), NULL)
INSERT [dbo].[BookAcquisitionRequests] ([request_id], [member_id], [title], [created_date], [author]) VALUES (2, 13, N'Cô gái d?n t? hôm qua', CAST(N'2026-06-28T19:00:58.883' AS DateTime), N'Nguy?n Nh?t Ánh')
INSERT [dbo].[BookAcquisitionRequests] ([request_id], [member_id], [title], [created_date], [author]) VALUES (3, 20, N'AAA', CAST(N'2026-07-03T22:37:56.367' AS DateTime), N'Takehiko Inoue')
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

INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (1, 1, N'Chí Phèo', N'9786040100003', N'Truy?n ng?n kinh di?n Vi?t Nam.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (2, 3, N'Clean Code', N'9786040100014', N'Sách g?i d?u giu?ng c?a l?p trình viên.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (3, 5, N'Sapiens: Lu?c S? Loài Ngu?i', N'9786040100010', N'L?ch s? ti?n hóa nhân lo?i.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (4, 11, N'Harry Potter và Hòn Ðá Phù Th?y', N'9786040100007', N'Ti?u thuy?t phép thu?t n?i ti?ng.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (5, 10, N'Ð?c Nhân Tâm', N'9786040100021', N'Ngh? thu?t thu ph?c lòng ngu?i.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (6, 1, N'Nhà Gi? Kim', N'9786040100038', N'Hành trình di tìm kho báu.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (7, 12, N'Phía Sau Nghi Can X', N'9786040100045', N'Trinh thám Nh?t B?n h?p d?n.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (8, 1, N'M?t Bi?c', N'9786040100052', N'Tình yêu tu?i h?c trò bu?n.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (9, 7, N'Ngu?i Giàu Có Nh?t Thành Babylon', N'9786040100069', N'Bí quy?t làm giàu t? xa xua.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (10, 2, N'D? Mèn Phiêu Luu Ký', N'9786040100076', N'Truy?n thi?u nhi kinh di?n.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (11, 3, N'Clean Architecture', N'9786040100083', N'Ki?n trúc ph?n m?m s?ch.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (12, 5, N'21 Bài H?c Cho Th? K? 21', N'9786040100090', N'Góc nhìn sâu s?c v? hi?n t?i.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (13, 11, N'Harry Potter và Phòng Ch?a Bí M?t', N'9786040100106', N'T?p 2 c?a Harry Potter.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (14, 1, N'Cho Tôi Xin M?t Vé Ði Tu?i Tho', N'9786040100113', N'Ký ?c tu?i tho tuoi d?p.', N'Active', NULL)
INSERT [dbo].[Books] ([book_id], [genre_id], [title], [isbn], [description], [status], [image]) VALUES (15, 12, N'B?ch D? Hành', N'9786040100120', N'Ti?u thuy?t trinh thám ám ?nh.', N'Active', NULL)
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

INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (1, N'Van h?c')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (2, N'Khoa h?c - Công ngh?')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (3, N'L?ch s? - Ð?a lý')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (4, N'Kinh t? - Tài chính')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (5, N'K? nang s?ng')
INSERT [dbo].[Categories] ([category_id], [category_name]) VALUES (6, N'Ti?u thuy?t - Hu c?u')
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
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (13, 12)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (16, 12)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (16, 15)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (17, 14)
INSERT [dbo].[Favorites] ([member_id], [book_id]) VALUES (20, 14)
GO
SET IDENTITY_INSERT [dbo].[Feedbacks] ON 

INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (1, 1, 1, 5, N'Tác ph?m kinh di?n, r?t ý nghia.', CAST(N'2026-06-19T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (2, 2, 5, 5, N'Ð?c xong th?y b?n thân thay d?i nhi?u.', CAST(N'2026-06-22T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (3, 4, 2, 4, N'Khá hay nhung hoi khó d?c cho ngu?i m?i.', CAST(N'2026-06-23T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (4, 6, 7, 5, N'C?t truy?n quá d?nh, không doán du?c cái k?t.', CAST(N'2026-06-24T20:01:08.300' AS DateTime), N'APPROVED', N'C?m on', CAST(N'2026-06-28T20:23:27.203' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (5, 8, 10, 5, N'Sách g?n li?n v?i tu?i tho.', CAST(N'2026-06-24T20:01:08.300' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (6, 4, 1, 5, N'Sách r?t hay, ki?n th?c b? ích!', CAST(N'2026-06-24T21:13:47.710' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (7, 13, 1, 3, N'Hay', CAST(N'2026-06-28T01:24:17.643' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (8, 13, 3, 1, N'Quá d?', CAST(N'2026-06-28T01:50:03.073' AS DateTime), N'APPROVED', N'Ok hehe', CAST(N'2026-06-28T20:23:10.113' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (9, 13, 6, 4, N'Hay l?m', CAST(N'2026-06-28T01:50:17.470' AS DateTime), N'APPROVED', N'Ok', CAST(N'2026-06-28T20:12:18.697' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (10, 13, 12, 5, N'Hay quá di', CAST(N'2026-06-28T17:43:36.710' AS DateTime), N'APPROVED', N'Ok c?m on', CAST(N'2026-06-28T17:47:04.447' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (11, 16, 5, 4, N'll', CAST(N'2026-06-30T16:07:09.857' AS DateTime), N'APPROVED', NULL, NULL)
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (12, 17, 4, 5, N'égrsdh', CAST(N'2026-07-01T22:48:25.477' AS DateTime), N'APPROVED', N'nguuu', CAST(N'2026-07-01T22:48:47.277' AS DateTime))
INSERT [dbo].[Feedbacks] ([feedback_id], [member_id], [book_id], [rating], [comment], [created_date], [status], [librarian_response], [response_date]) VALUES (17, 13, 13, 4, N'ád', CAST(N'2026-07-03T23:31:29.420' AS DateTime), N'APPROVED', NULL, NULL)
SET IDENTITY_INSERT [dbo].[Feedbacks] OFF
GO
SET IDENTITY_INSERT [dbo].[Genres] ON 

INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (1, 1, N'Ti?u thuy?t van h?c')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (2, 1, N'Truy?n ng?n')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (3, 2, N'Công ngh? thông tin')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (4, 2, N'Khoa h?c vu tr?')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (5, 3, N'L?ch s? th? gi?i')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (6, 3, N'L?ch s? Vi?t Nam')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (7, 4, N'Ð?u tu tài chính')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (8, 4, N'Kinh doanh kh?i nghi?p')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (9, 5, N'Tâm lý h?c')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (10, 5, N'Phát tri?n b?n thân')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (11, 6, N'Hành d?ng - K? ?o')
INSERT [dbo].[Genres] ([genre_id], [category_id], [genre_name]) VALUES (12, 6, N'Trinh thám')
SET IDENTITY_INSERT [dbo].[Genres] OFF
GO
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (1, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (2, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (3, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (4, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (5, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (6, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (7, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (8, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (9, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (10, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (11, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (12, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (18, 3)
INSERT [dbo].[Member_Account_Roles] ([member_account_id], [role_id]) VALUES (19, 3)
GO
SET IDENTITY_INSERT [dbo].[Member_Accounts] ON 

INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (1, 1, N'member01', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (2, 2, N'member02', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (3, 3, N'member03', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (4, 4, N'member04', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (5, 5, N'member05', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (6, 6, N'member06', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (7, 7, N'member07', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (8, 8, N'member08', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (9, 9, N'member09', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (10, 10, N'member10', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (11, 11, N'member11', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (12, 12, N'member12', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (13, 13, N'quocanh', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (14, 14, N'wusenminu', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (15, 15, N'sa', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (16, 16, N'nhoc2323@gmail.com', N'', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (17, 17, N'test1', N'$2a$10$OiRgm5HFnPF55ivgfoWAyuwykjkh5lKVhaWwK92bPmEjL2QrE2p5a', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (18, 18, N'test2', N'$2a$10$v00NLAXLiK2NZaA2DqHOU.t.kZmASSI9kIhVrPBQAfxPFq9mIJB4q', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (19, 19, N'thuongnt.ce191329@gmail.com', N'', N'Active')
INSERT [dbo].[Member_Accounts] ([id], [member_id], [username], [password_hash], [status]) VALUES (20, 20, N'qanh', N'$2a$10$b/7sMpeGmfRfYsRB7216c.f0NoiL2DcIqtf34MMPVkOqnXxCh41du', N'Active')
SET IDENTITY_INSERT [dbo].[Member_Accounts] OFF
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
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 8, 1, CAST(N'2026-07-03T23:31:19.190' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 9, 1, CAST(N'2026-07-03T23:31:19.190' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 10, 1, CAST(N'2026-07-03T23:31:19.190' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 12, 1, CAST(N'2026-07-03T23:31:19.190' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (13, 13, 1, CAST(N'2026-07-03T23:31:19.190' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (17, 14, 1, CAST(N'2026-07-01T22:49:01.047' AS DateTime))
INSERT [dbo].[MemberNotifications] ([member_id], [notification_id], [is_read], [read_date]) VALUES (20, 15, 1, CAST(N'2026-07-03T22:39:23.400' AS DateTime))
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
INSERT [dbo].[Members] ([member_id], [user_id], [tier_id]) VALUES (20, 23, 1)
SET IDENTITY_INSERT [dbo].[Members] OFF
GO
SET IDENTITY_INSERT [dbo].[MembershipTiers] ON 

INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (1, N'Regular', CAST(0.00 AS Decimal(5, 2)), 3, CAST(0.00 AS Decimal(18, 2)), N'Mu?n t?i da 3 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (2, N'Silver', CAST(10.00 AS Decimal(5, 2)), 5, CAST(500000.00 AS Decimal(18, 2)), N'Gi?m 10% phí mu?n, mu?n t?i da 5 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (3, N'Gold', CAST(20.00 AS Decimal(5, 2)), 10, CAST(1000000.00 AS Decimal(18, 2)), N'Gi?m 20% phí mu?n, mu?n t?i da 10 sách')
INSERT [dbo].[MembershipTiers] ([tier_id], [tier_name], [discount_percent], [borrow_limit], [condition], [benefits]) VALUES (4, N'Diamond', CAST(50.00 AS Decimal(5, 2)), 20, CAST(5000000.00 AS Decimal(18, 2)), N'Gi?m 50% phí mu?n, mu?n t?i da 20 sách')
SET IDENTITY_INSERT [dbo].[MembershipTiers] OFF
GO
SET IDENTITY_INSERT [dbo].[Notifications] ON 

INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (2, NULL, N'Test Thông báo', N'thông báo t?i toàn member', CAST(N'2026-06-27T21:15:04.470' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (3, NULL, N'abc', N'123', CAST(N'2026-06-27T21:33:16.973' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (8, NULL, N'Ph?n h?i dánh giá', N'Th? thu dã ph?n h?i dánh giá c?a b?n cho sách ''21 Bài H?c Cho Th? K? 21''.', CAST(N'2026-06-28T17:47:04.453' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (9, NULL, N'Ph?n h?i dánh giá', N'Th? thu dã ph?n h?i dánh giá c?a b?n cho sách ''Nhà Gi? Kim''.', CAST(N'2026-06-28T20:12:18.707' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (10, NULL, N'Ph?n h?i dánh giá', N'Th? thu dã ph?n h?i dánh giá c?a b?n cho sách ''Sapiens: Lu?c S? Loài Ngu?i''.', CAST(N'2026-06-28T20:23:10.123' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (11, NULL, N'Ph?n h?i dánh giá', N'Th? thu dã ph?n h?i dánh giá c?a b?n cho sách ''Phía Sau Nghi Can X''.', CAST(N'2026-06-28T20:23:27.203' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (12, NULL, N'abc', N'abc', CAST(N'2026-06-28T20:23:55.437' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (13, NULL, N'M ngu', N'abc', CAST(N'2026-06-28T21:13:36.080' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (14, NULL, N'Ph?n h?i dánh giá', N'Th? thu dã ph?n h?i dánh giá c?a b?n cho sách ''Harry Potter và Hòn Ðá Phù Th?y''.', CAST(N'2026-07-01T22:48:47.280' AS DateTime), N'Active')
INSERT [dbo].[Notifications] ([notification_id], [staff_id], [title], [content], [created_date], [status]) VALUES (15, NULL, N'Test Thông Báo', N'123', CAST(N'2026-07-03T22:39:13.757' AS DateTime), N'Active')
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

INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (1, N'K? A1 - Van h?c VN', N'T?ng 1 - Khu A')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (2, N'K? A2 - Van h?c NN', N'T?ng 1 - Khu A')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (3, N'K? B1 - IT', N'T?ng 2 - Khu B')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (4, N'K? C1 - K? nang', N'T?ng 3 - Khu C')
INSERT [dbo].[Shelves] ([shelf_id], [shelf_name], [location]) VALUES (5, N'K? D1 - Trinh thám', N'T?ng 2 - Khu D')
SET IDENTITY_INSERT [dbo].[Shelves] OFF
GO
SET IDENTITY_INSERT [dbo].[Staff] ON 

INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (1, 1, N'Admin')
INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (2, 2, N'Librarian')
INSERT [dbo].[Staff] ([staff_id], [user_id], [staff_type]) VALUES (3, 3, N'Librarian')
SET IDENTITY_INSERT [dbo].[Staff] OFF
GO
INSERT [dbo].[Staff_Account_Roles] ([staff_account_id], [role_id]) VALUES (1, 1)
INSERT [dbo].[Staff_Account_Roles] ([staff_account_id], [role_id]) VALUES (2, 2)
INSERT [dbo].[Staff_Account_Roles] ([staff_account_id], [role_id]) VALUES (3, 2)
GO
SET IDENTITY_INSERT [dbo].[Staff_Accounts] ON 

INSERT [dbo].[Staff_Accounts] ([id], [staff_id], [username], [password_hash], [status]) VALUES (1, 1, N'admin', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve.', N'Active')
INSERT [dbo].[Staff_Accounts] ([id], [staff_id], [username], [password_hash], [status]) VALUES (2, 2, N'librarian01', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve.', N'Active')
INSERT [dbo].[Staff_Accounts] ([id], [staff_id], [username], [password_hash], [status]) VALUES (3, 3, N'librarian02', N'$2a$10$ms3v5xqzpJCLxKyzQVPLjuYA3b6jIKMnY8SsnfKlJsSJPMN7A3ve', N'Active')
SET IDENTITY_INSERT [dbo].[Staff_Accounts] OFF
GO
SET IDENTITY_INSERT [dbo].[SystemLogs] ON 

INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (1, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 5D0E0E199EE2C20E8F7546CD549961E8', CAST(N'2026-06-30T16:06:39.070' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (2, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 01C89BC503E9CE169B12DF04F3C8D1DE', CAST(N'2026-06-30T16:08:16.140' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (3, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 01C89BC503E9CE169B12DF04F3C8D1DE', CAST(N'2026-06-30T16:08:56.320' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (4, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 2B4E36BA44D37874373EBD7A12E5778B', CAST(N'2026-06-30T16:08:56.537' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (5, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: A3199CBD16166D1A0E2917BFDB3700D6', CAST(N'2026-06-30T16:11:10.587' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (6, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 0168C839C9CD28D6DD057C0F08DEA1C2', CAST(N'2026-07-01T22:06:58.617' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (7, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 9D493B32BA6C73E50C6B4282B3073AB8', CAST(N'2026-07-01T22:07:01.237' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (8, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 9D493B32BA6C73E50C6B4282B3073AB8', CAST(N'2026-07-01T22:32:23.117' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (9, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 8E849DA2A13BACBA883822926F86520A', CAST(N'2026-07-01T22:32:27.090' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (10, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 8E849DA2A13BACBA883822926F86520A', CAST(N'2026-07-01T22:38:29.997' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (11, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: E8E990AA2AE94D818B4F102F18F82556', CAST(N'2026-07-01T22:39:12.237' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (12, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: E8E990AA2AE94D818B4F102F18F82556', CAST(N'2026-07-01T22:39:21.440' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (13, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 375F3B28E1AE9D20B020EF8413B691DA', CAST(N'2026-07-01T22:39:45.947' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (14, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: A8620FFE9CE99B755D935E1D23040865', CAST(N'2026-07-01T22:39:47.947' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (15, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: A8620FFE9CE99B755D935E1D23040865', CAST(N'2026-07-01T22:40:32.753' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (16, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 77C272BE4883684A3ECDF89747A71B4D', CAST(N'2026-07-01T22:40:49.990' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (17, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 77C272BE4883684A3ECDF89747A71B4D', CAST(N'2026-07-01T22:40:52.263' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (18, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 5466FCE4A70B89BF314ABA3206F269FE', CAST(N'2026-07-01T22:41:10.947' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (19, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 75CE20589A15B8D353735879C9184467', CAST(N'2026-07-01T22:41:19.677' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (20, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 75CE20589A15B8D353735879C9184467', CAST(N'2026-07-01T22:43:11.160' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (21, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: D27B2E914A58A1C0808BFDBED6C03A1F', CAST(N'2026-07-01T22:47:56.613' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (22, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 7EFA7EF66180DB4932D3AA93F0E403BC', CAST(N'2026-07-01T22:48:28.237' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (23, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 7EFA7EF66180DB4932D3AA93F0E403BC', CAST(N'2026-07-01T22:48:37.480' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (24, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 2F632D94E77D2B554C32921EAF6D0E02', CAST(N'2026-07-01T22:48:49.090' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (25, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 2F632D94E77D2B554C32921EAF6D0E02', CAST(N'2026-07-01T22:48:54.477' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (26, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: F436DB2583189B7D5A36C50EBCB948E4', CAST(N'2026-07-01T22:51:07.293' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (27, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: AD43F3FCB936828E44BB183FF98F775A', CAST(N'2026-07-01T23:18:19.973' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (28, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: CC162781615C2936E7198A21B865CCCF', CAST(N'2026-07-01T23:19:09.510' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (29, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: CC162781615C2936E7198A21B865CCCF', CAST(N'2026-07-01T23:21:06.647' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (30, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: DB03C859CCECBBD2D1989C71D8FC9F9D', CAST(N'2026-07-01T23:21:34.253' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (31, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: DB03C859CCECBBD2D1989C71D8FC9F9D', CAST(N'2026-07-01T23:21:39.010' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (32, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 3AADEBE80089AD041886FA17CB80299F', CAST(N'2026-07-01T23:21:56.890' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (33, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 3AADEBE80089AD041886FA17CB80299F', CAST(N'2026-07-01T23:27:50.157' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (34, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 85A3679B0C23B633D0543A80ADA50ACA', CAST(N'2026-07-02T00:03:13.420' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (35, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 8CEE674E60534FC9847F389E0BA3EA99', CAST(N'2026-07-02T00:07:10.680' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (36, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 8CEE674E60534FC9847F389E0BA3EA99', CAST(N'2026-07-02T00:07:28.377' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (37, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 3EF36B1C2223307A85EE7859CC19CCFF', CAST(N'2026-07-02T00:09:15.350' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (38, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 3EF36B1C2223307A85EE7859CC19CCFF', CAST(N'2026-07-02T00:09:46.337' AS DateTime), 21)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (39, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: F91F750763AF89A3829EAFCD9A2371F0', CAST(N'2026-07-02T00:37:33.503' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (40, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: A8C88F8642FAE73F017938BAE802ACBB', CAST(N'2026-07-02T00:38:57.447' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (41, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: A8C88F8642FAE73F017938BAE802ACBB', CAST(N'2026-07-02T01:04:19.440' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (42, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 88D5AD911E3D44DA7ECF7DA944966D0F', CAST(N'2026-07-02T01:06:04.433' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (43, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 88D5AD911E3D44DA7ECF7DA944966D0F', CAST(N'2026-07-02T01:06:09.777' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (44, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: E8B70489C1B24917BA560D84343C557C', CAST(N'2026-07-02T01:06:28.860' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (45, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: E8B70489C1B24917BA560D84343C557C', CAST(N'2026-07-02T01:06:37.197' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (46, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 57618B29755E4007DD745BF3AC57B304', CAST(N'2026-07-02T01:07:15.157' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (47, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 57618B29755E4007DD745BF3AC57B304', CAST(N'2026-07-02T01:07:19.903' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (48, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: DA3F279318BC3238100DD91B8ADB213D', CAST(N'2026-07-02T02:11:09.470' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (49, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: B860176E5E8F9D69E3055BBC4BB8F328', CAST(N'2026-07-02T02:11:14.820' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (50, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: B860176E5E8F9D69E3055BBC4BB8F328', CAST(N'2026-07-02T02:11:22.023' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (51, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 9C82090947A895F3B97E8ADBAE550A1D', CAST(N'2026-07-02T13:06:23.730' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (52, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 4C4732E60B9306D0B75D27071C891CFC', CAST(N'2026-07-02T13:06:32.330' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (53, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 4C4732E60B9306D0B75D27071C891CFC', CAST(N'2026-07-02T13:06:40.247' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (54, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 048C73770553BBDA16402CAD7C104237', CAST(N'2026-07-02T13:06:45.283' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (55, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 048C73770553BBDA16402CAD7C104237', CAST(N'2026-07-02T13:06:48.443' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (56, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 752FF408ABC0CF4C551D2DC84F45FBAB', CAST(N'2026-07-02T13:07:07.270' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (57, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 752FF408ABC0CF4C551D2DC84F45FBAB', CAST(N'2026-07-02T13:07:19.053' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (58, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 7F7252D98407FEF8931C525D6677CFF6', CAST(N'2026-07-02T13:07:26.787' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (59, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 7F7252D98407FEF8931C525D6677CFF6', CAST(N'2026-07-02T13:19:21.987' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (60, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 4A6476B5A724274D38507E92A43892E5', CAST(N'2026-07-02T13:25:03.977' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (61, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 4A6476B5A724274D38507E92A43892E5', CAST(N'2026-07-02T13:25:08.370' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (62, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: DC4F4C55A1C303B37BD31008E3D42301', CAST(N'2026-07-02T13:31:07.603' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (63, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: DC4F4C55A1C303B37BD31008E3D42301', CAST(N'2026-07-02T13:32:39.370' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (64, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 9F29F1679B7FC46D89A57ABE29873B1E', CAST(N'2026-07-02T13:38:12.783' AS DateTime), 20)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (65, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 9F29F1679B7FC46D89A57ABE29873B1E', CAST(N'2026-07-02T13:38:18.007' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (66, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: BBF475A847AAD885BEC60429642A3377', CAST(N'2026-07-02T13:38:24.840' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (67, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: BBF475A847AAD885BEC60429642A3377', CAST(N'2026-07-02T13:38:47.057' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (68, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 5DE9557D519AF1DD348EAFA3CFC49938', CAST(N'2026-07-02T13:44:37.580' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (69, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: C360E99FC6EDCF5657669085072B6F14', CAST(N'2026-07-02T14:14:33.530' AS DateTime), 22)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (70, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: CB7A02010BE334130EB65CBF2C0E722F', CAST(N'2026-07-03T03:40:30.350' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (71, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: C27FA9705CF310300CC83B9AF2C85BB2', CAST(N'2026-07-03T03:40:47.697' AS DateTime), 1)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (72, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: C27FA9705CF310300CC83B9AF2C85BB2', CAST(N'2026-07-03T03:40:52.567' AS DateTime), 22)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (73, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: F6BFB60FF9C575FF06B31A6EFD253DD9', CAST(N'2026-07-03T16:18:14.090' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (74, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 0EF0E0C274CCEE436D91B9BDFD5031EA', CAST(N'2026-07-03T17:18:15.673' AS DateTime), 19)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (75, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: E82728D176CD585C09BD5A2C99A520A3', CAST(N'2026-07-03T22:19:09.827' AS DateTime), 16)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (76, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: EF4CB55E1E91E71BA74ED6EBE63BB8A9', CAST(N'2026-07-03T22:19:18.520' AS DateTime), 16)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (77, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: EF4CB55E1E91E71BA74ED6EBE63BB8A9', CAST(N'2026-07-03T22:19:36.510' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (78, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: B60FEE77F0B5C7E81529037A4E456782', CAST(N'2026-07-03T22:33:17.500' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (79, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: B60FEE77F0B5C7E81529037A4E456782', CAST(N'2026-07-03T22:37:32.147' AS DateTime), 23)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (80, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 19B173C4FE88A33776EAF0C19310332C', CAST(N'2026-07-03T22:38:26.577' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (81, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 7BF628DE00A9D6B77956D8C99A8F3E12', CAST(N'2026-07-03T23:00:11.363' AS DateTime), 23)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (82, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 19B173C4FE88A33776EAF0C19310332C', CAST(N'2026-07-03T23:03:33.637' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (83, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 3B81FE05380580BD9E7C4C3F0E417229', CAST(N'2026-07-03T23:14:44.257' AS DateTime), 23)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (84, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 905BF2313CBF0282D494F088C727E376', CAST(N'2026-07-03T23:29:56.270' AS DateTime), 2)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (85, N'LOGOUT', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang xu?t thành công. Session ID: 22785586438BC1754337D93ACCDF8AC7', CAST(N'2026-07-03T23:31:09.713' AS DateTime), 23)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (86, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 22785586438BC1754337D93ACCDF8AC7', CAST(N'2026-07-03T23:31:16.063' AS DateTime), 16)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (87, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: 1FF20F28024A77EA18A4D7334BF1FDE2', CAST(N'2026-07-03T23:40:59.490' AS DateTime), 23)
INSERT [dbo].[SystemLogs] ([log_id], [action_type], [ip_address], [user_agent], [description], [created_at], [user_id]) VALUES (88, N'LOGIN', N'0:0:0:0:0:0:0:1', N'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36', N'Ðang nh?p thành công. Session ID: D2217261BDB26474E2F19E8C2ED516B7', CAST(N'2026-07-03T23:41:57.660' AS DateTime), 23)
SET IDENTITY_INSERT [dbo].[SystemLogs] OFF
GO
SET IDENTITY_INSERT [dbo].[SystemSettings] ON 

INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (1, N'Fine_Per_Day', N'5000', N'Phí ph?t tr? h?n tính theo ngày (VND)')
INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (2, N'Max_Borrow_Days', N'14', N'S? ngày mu?n sách t?i da tiêu chu?n')
INSERT [dbo].[SystemSettings] ([setting_id], [setting_key], [setting_value], [description]) VALUES (3, N'Deposit_Amount', N'50000', N'Ti?n c?c d?t tru?c m?t quy?n sách (VND)')
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

INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (1, N'Admin T?ng', N'admin@library.vn', N'0901000001', N'Active', N'https://res.cloudinary.com/m7dcz22q/image/upload/v1782929184/ezvohujlkyqx1sghj5hh.png')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (2, N'Nguy?n Th? Lan', N'lib01@library.vn', N'', N'Active', N'https://res.cloudinary.com/m7dcz22q/image/upload/v1782929231/q0aymj0exsg4yflwiono.png')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (3, N'Tr?n Van Minh', N'lib02@library.vn', N'0902000002', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (4, N'Lê Th? Hoa', N'member01@gmail.com', N'0903000001', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (5, N'Ph?m Minh Tu?n', N'member02@gmail.com', N'0903000002', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (6, N'Nguy?n Van An', N'member03@gmail.com', N'0903000003', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (7, N'Tr?n Th? Mai', N'member04@gmail.com', N'0903000004', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (8, N'Hoàng Van Dung', N'member05@gmail.com', N'0903000005', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (9, N'Ð?ng Thu Hà', N'member06@gmail.com', N'0903000006', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (10, N'Vu Ð?c Cu?ng', N'member07@gmail.com', N'0903000007', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (11, N'Bùi Th? Thanh', N'member08@gmail.com', N'0903000008', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (12, N'Ð? Xuân Hùng', N'member09@gmail.com', N'0903000009', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (13, N'Ngô B?o Châu', N'member10@gmail.com', N'0903000010', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (14, N'Lý H?i Y?n', N'member11@gmail.com', N'0903000011', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (15, N'Duong T?n Sang', N'member12@gmail.com', N'0903000012', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (16, N'Qu?c Anh', N'tnquocanh.ce191655@gmail.com', N'0856989555', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (17, N'Th? Hi?n', N'quocanh26032005@gmail.com', N'0856989555', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (18, N'uuuwuwuuw', N'dantnl.ce191088@gmail.com', N'08569895553', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (19, N'Thuong Nguy?n', N'nhoc2323@gmail.com', N'0123456789', N'Active', N'https://res.cloudinary.com/m7dcz22q/image/upload/v1782929278/htgihr7fvpehnirwy4vh.png')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (20, N'Nguyen Tien Thuong', N'thuong28092005@gmail.com', N'0907648667', N'Active', N'https://res.cloudinary.com/m7dcz22q/image/upload/v1782929137/cda7sgyiqns4otavotrl.png')
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (21, N'Ð? Xuân Hùng', N'thuong1@gmail.com', N'0907648667', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (22, N'Ti?n Thuong Nguy?n', N'thuongnt.ce191329@gmail.com', N'', N'Active', NULL)
INSERT [dbo].[Users] ([user_id], [full_name], [email], [phone], [status], [avatar]) VALUES (23, N'Tr?n Nguy?n Qu?c Anh', N'tnquocanh@gmail.com', N'0856989555', N'Active', NULL)
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
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (16, 16, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (17, 17, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (18, 18, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (19, 19, CAST(0.00 AS Decimal(18, 2)))
INSERT [dbo].[Wallets] ([wallet_id], [member_id], [balance]) VALUES (20, 20, CAST(0.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[Wallets] OFF
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__BookItem__C16E36F8BBAC7E44]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[BookItems] ADD UNIQUE NONCLUSTERED 
(
	[barcode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Books__99F9D0A4455F5171]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[Books] ADD UNIQUE NONCLUSTERED 
(
	[isbn] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Member_A__F3DBC572947BCE32]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[Member_Accounts] ADD UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Password__CA90DA7AAD2D2541]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[PasswordResetTokens] ADD UNIQUE NONCLUSTERED 
(
	[token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Roles__72E12F1B801B0D75]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[Roles] ADD UNIQUE NONCLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Staff_Ac__F3DBC572B5D2BD8F]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[Staff_Accounts] ADD UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__SystemSe__0DFAC4270425D6EF]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[SystemSettings] ADD UNIQUE NONCLUSTERED 
(
	[setting_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__Users__AB6E6164BAC2023F]    Script Date: 03/07/2026 11:44:19 pm ******/
ALTER TABLE [dbo].[Users] ADD UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
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
ALTER TABLE [dbo].[Member_Accounts] ADD  DEFAULT ('Active') FOR [status]
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
ALTER TABLE [dbo].[Staff_Accounts] ADD  DEFAULT ('Active') FOR [status]
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
ALTER TABLE [dbo].[Member_Account_Roles]  WITH CHECK ADD FOREIGN KEY([member_account_id])
REFERENCES [dbo].[Member_Accounts] ([id])
GO
ALTER TABLE [dbo].[Member_Account_Roles]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[Roles] ([role_id])
GO
ALTER TABLE [dbo].[Member_Accounts]  WITH CHECK ADD FOREIGN KEY([member_id])
REFERENCES [dbo].[Members] ([member_id])
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
ALTER TABLE [dbo].[PasswordResetTokens]  WITH CHECK ADD  CONSTRAINT [FK_PasswordResetTokens_Users] FOREIGN KEY([user_id])
REFERENCES [dbo].[Users] ([user_id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[PasswordResetTokens] CHECK CONSTRAINT [FK_PasswordResetTokens_Users]
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
ALTER TABLE [dbo].[Staff_Account_Roles]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[Roles] ([role_id])
GO
ALTER TABLE [dbo].[Staff_Account_Roles]  WITH CHECK ADD FOREIGN KEY([staff_account_id])
REFERENCES [dbo].[Staff_Accounts] ([id])
GO
ALTER TABLE [dbo].[Staff_Accounts]  WITH CHECK ADD FOREIGN KEY([staff_id])
REFERENCES [dbo].[Staff] ([staff_id])
GO
ALTER TABLE [dbo].[SystemLogs]  WITH CHECK ADD  CONSTRAINT [FK_SystemLogs_Users] FOREIGN KEY([user_id])
REFERENCES [dbo].[Users] ([user_id])
GO
ALTER TABLE [dbo].[SystemLogs] CHECK CONSTRAINT [FK_SystemLogs_Users]
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
USE [master]
GO
ALTER DATABASE [tes] SET  READ_WRITE 
GO
