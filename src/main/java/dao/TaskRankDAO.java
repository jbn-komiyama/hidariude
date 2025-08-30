package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.TaskRankDTO;

public class TaskRankDAO extends BaseDAO {
	
	// ========================
    // SQL 定義
    // ========================
    /** 削除されていないランクの基本SELECT（id, rank_name, base_pay_* ほか） */
	
    private static final String SQL_SELECT_BASE =
        "SELECT id, rank_name, base_pay_customer, base_pay_secretary, "
      + "       created_at, updated_at, deleted_at "
      + "  FROM task_rank "
      + " WHERE deleted_at IS NULL";

    /** PM想定ランク（rank_no = 0）を取得するための条件 */
    private static final String SQL_WHERE_PM = " AND rank_no = 0";
	
	public TaskRankDAO(Connection conn) {
		super(conn);
	}
	
	public List<TaskRankDTO> selectAll() {
		List<TaskRankDTO> dtos = new ArrayList<>();
		
		try {
			PreparedStatement ps = conn.prepareStatement(
					SQL_SELECT_BASE);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				TaskRankDTO dto = new TaskRankDTO();
				int i = 1;
				dto.setId(rs.getObject(i++, UUID.class)); 
				dto.setRankName(rs.getString(i++)); 
				dto.setBasePayCustomer(rs.getBigDecimal(i++)); 
				dto.setBasePaySecretary(rs.getBigDecimal(i++)); 
				dto.setCreatedAt(rs.getTimestamp(i++)); 
				dto.setUpdatedAt(rs.getTimestamp(i++)); 
				dto.setDeletedAt(rs.getTimestamp(i++)); 
				dtos.add(dto);
			}
			return dtos;
		} catch(SQLException e) {
			String errorMsg = "E:B11 Secretaryテーブルに不正なSELECT処理が行われました。";
			e.printStackTrace();
			throw new DAOException(errorMsg, e);
		}
	}
	
	public TaskRankDTO selectPM() {
		TaskRankDTO dto = new TaskRankDTO();
		
		try {
			PreparedStatement ps = conn.prepareStatement(
					SQL_SELECT_BASE + SQL_WHERE_PM);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				int i = 1;
				dto.setId(rs.getObject(i++, UUID.class)); 
				dto.setRankName(rs.getString(i++)); 
				dto.setBasePayCustomer(rs.getBigDecimal(i++)); 
				dto.setBasePaySecretary(rs.getBigDecimal(i++)); 
				dto.setCreatedAt(rs.getTimestamp(i++)); 
				dto.setUpdatedAt(rs.getTimestamp(i++)); 
				dto.setDeletedAt(rs.getTimestamp(i++)); 
			}
			return dto;
		} catch(SQLException e) {
			String errorMsg = "E:B11 Secretaryテーブルに不正なSELECT処理が行われました。";
			e.printStackTrace();
			throw new DAOException(errorMsg, e);
		}
	}
}