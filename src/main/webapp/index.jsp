<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BackDesk - ログイン選択</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #e8f4f8 0%, #f0f8ff 50%, #e6f2ff 100%);
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .container {
            background: #ffffff;
            padding: 2.5rem;
            border-radius: 20px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
            border: 1px solid rgba(0, 0, 0, 0.06);
            text-align: center;
            max-width: 400px;
            width: 90%;
        }
        .logo {
            font-size: 2.2rem;
            font-weight: 700;
            color: #2c3e50;
            margin-bottom: 0.5rem;
            letter-spacing: 2px;
        }
        .subtitle {
            color: #6c757d;
            margin-bottom: 2.5rem;
            font-size: 0.95rem;
        }
        .login-options {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }
        .login-btn {
            display: block;
            text-decoration: none;
            padding: 1.2rem;
            border-radius: 12px;
            font-size: 1.05rem;
            font-weight: 600;
            transition: all 0.3s ease;
            color: #ffffff;
            position: relative;
            overflow: hidden;
            letter-spacing: 0.5px;
        }
        .login-btn::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255,255,255,0.15);
            transform: translateX(-100%);
            transition: transform 0.3s ease;
        }
        .login-btn:hover::before {
            transform: translateX(0);
        }
        .admin-btn {
            background: linear-gradient(135deg, #dc3545, #c82333);
            box-shadow: 0 4px 12px rgba(220, 53, 69, 0.25);
        }
        .admin-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(220, 53, 69, 0.35);
        }
        .secretary-btn {
            background: linear-gradient(135deg, #20c997, #17a589);
            box-shadow: 0 4px 12px rgba(32, 201, 151, 0.25);
        }
        .secretary-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(32, 201, 151, 0.35);
        }
        .customer-btn {
            background: linear-gradient(135deg, #0d6efd, #0b5ed7);
            box-shadow: 0 4px 12px rgba(13, 110, 253, 0.25);
        }
        .customer-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(13, 110, 253, 0.35);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">BackDesk</div>
        <div class="subtitle">役割を選択してログインしてください</div>
        
        <div class="login-options">
            <a href="<%= request.getContextPath() %>/admin" class="login-btn admin-btn">
                管理者ログイン
            </a>
            
            <a href="<%= request.getContextPath() %>/secretary" class="login-btn secretary-btn">
                秘書ログイン
            </a>
            
            <a href="<%= request.getContextPath() %>/customer" class="login-btn customer-btn">
                顧客ログイン
            </a>
        </div>
    </div>
</body>
</html>
