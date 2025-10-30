package filter;

import java.io.IOException;
import java.util.List;

import dao.TaskDAO;
import dao.TransactionManager;
import domain.LoginUser;
import dto.TaskDTO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 管理者画面でアラート件数をrequestスコープにセットするFilter。
 * ナビバーでアラートバッジを表示するために使用します。
 */
@WebFilter("/admin/*")
public class AlertCountFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初期化処理（特になし）
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 管理者がログインしているか確認
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("loginUser") != null) {
            LoginUser loginUser = (LoginUser) session.getAttribute("loginUser");
            
            // 管理者の場合のみアラート件数を取得
            if (loginUser.getSystemAdmin() != null) {
                try (TransactionManager tm = new TransactionManager()) {
                    TaskDAO dao = new TaskDAO(tm.getConnection());
                    // showAlert(false): 全件取得
                    List<TaskDTO> alerts = dao.showAlert(false);
                    // アラート件数をrequestスコープにセット
                    request.setAttribute("alertCount", alerts.size());
                } catch (Exception e) {
                    // エラーが発生してもフィルタ処理は継続
                    e.printStackTrace();
                    request.setAttribute("alertCount", 0);
                }
            }
        }
        
        // 次のフィルタまたはサーブレットへ
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // クリーンアップ処理（特になし）
    }
}

