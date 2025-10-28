<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>秘書管理 - ログイン選択</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .container {
            background: rgba(26, 26, 46, 0.9);
            backdrop-filter: blur(10px);
            padding: 2.5rem;
            border-radius: 20px;
            box-shadow: 0 25px 50px rgba(0,0,0,0.5);
            border: 1px solid rgba(255,255,255,0.1);
            text-align: center;
            max-width: 400px;
            width: 90%;
        }
        .logo {
            font-size: 2.2rem;
            font-weight: 700;
            color: #e8e8e8;
            margin-bottom: 0.5rem;
            letter-spacing: 2px;
        }
        .subtitle {
            color: #b0b0b0;
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
            color: #f0f0f0;
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
            background: rgba(255,255,255,0.1);
            transform: translateX(-100%);
            transition: transform 0.3s ease;
        }
        .login-btn:hover::before {
            transform: translateX(0);
        }
        .admin-btn {
            background: linear-gradient(135deg, #8b1a1a, #b91d1d);
            box-shadow: 0 4px 15px rgba(139, 26, 26, 0.3);
        }
        .admin-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 12px 25px rgba(185, 29, 29, 0.4);
        }
        .secretary-btn {
            background: linear-gradient(135deg, #1a5653, #1e7a76);
            box-shadow: 0 4px 15px rgba(26, 86, 83, 0.3);
        }
        .secretary-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 12px 25px rgba(30, 122, 118, 0.4);
        }
        .customer-btn {
            background: linear-gradient(135deg, #1a4971, #1e5a8f);
            box-shadow: 0 4px 15px rgba(26, 73, 113, 0.3);
        }
        .customer-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 12px 25px rgba(30, 90, 143, 0.4);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">秘書管理</div>
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
