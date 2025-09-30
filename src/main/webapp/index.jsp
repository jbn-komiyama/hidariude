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
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .container {
            background: white;
            padding: 2rem;
            border-radius: 15px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            text-align: center;
            max-width: 400px;
            width: 90%;
        }
        .logo {
            font-size: 2rem;
            font-weight: bold;
            color: #333;
            margin-bottom: 1rem;
        }
        .subtitle {
            color: #666;
            margin-bottom: 2rem;
        }
        .login-options {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }
        .login-btn {
            display: block;
            text-decoration: none;
            padding: 1rem;
            border-radius: 10px;
            font-size: 1.1rem;
            font-weight: 500;
            transition: all 0.3s ease;
            color: white;
        }
        .admin-btn {
            background: linear-gradient(45deg, #ff6b6b, #ee5a52);
        }
        .admin-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(255,107,107,0.3);
        }
        .secretary-btn {
            background: linear-gradient(45deg, #4ecdc4, #44a08d);
        }
        .secretary-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(78,205,196,0.3);
        }
        .customer-btn {
            background: linear-gradient(45deg, #45b7d1, #96c93d);
        }
        .customer-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(69,183,209,0.3);
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
