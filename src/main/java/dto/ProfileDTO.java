package dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 秘書プロフィール（profiles）テーブルのDTO
 */
public class ProfileDTO implements Serializable{
	private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID secretaryId;

    private Integer weekdayMorning;
    private Integer weekdayDaytime;
    private Integer weekdayNight;

    private Integer saturdayMorning;
    private Integer saturdayDaytime;
    private Integer saturdayNight;

    private Integer sundayMorning;
    private Integer sundayDaytime;
    private Integer sundayNight;

    private BigDecimal weekdayWorkHours;
    private BigDecimal saturdayWorkHours;
    private BigDecimal sundayWorkHours;

    private BigDecimal monthlyWorkHours;

    private String remark;
    private String qualification;
    private String workHistory;
    private String academicBackground;
    private String selfIntroduction;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

    /** --- getter / setter（省略なし） --- */
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSecretaryId() { return secretaryId; }
    public void setSecretaryId(UUID secretaryId) { this.secretaryId = secretaryId; }
    public Integer getWeekdayMorning() { return weekdayMorning; }
    public void setWeekdayMorning(Integer weekdayMorning) { this.weekdayMorning = weekdayMorning; }
    public Integer getWeekdayDaytime() { return weekdayDaytime; }
    public void setWeekdayDaytime(Integer weekdayDaytime) { this.weekdayDaytime = weekdayDaytime; }
    public Integer getWeekdayNight() { return weekdayNight; }
    public void setWeekdayNight(Integer weekdayNight) { this.weekdayNight = weekdayNight; }
    public Integer getSaturdayMorning() { return saturdayMorning; }
    public void setSaturdayMorning(Integer saturdayMorning) { this.saturdayMorning = saturdayMorning; }
    public Integer getSaturdayDaytime() { return saturdayDaytime; }
    public void setSaturdayDaytime(Integer saturdayDaytime) { this.saturdayDaytime = saturdayDaytime; }
    public Integer getSaturdayNight() { return saturdayNight; }
    public void setSaturdayNight(Integer saturdayNight) { this.saturdayNight = saturdayNight; }
    public Integer getSundayMorning() { return sundayMorning; }
    public void setSundayMorning(Integer sundayMorning) { this.sundayMorning = sundayMorning; }
    public Integer getSundayDaytime() { return sundayDaytime; }
    public void setSundayDaytime(Integer sundayDaytime) { this.sundayDaytime = sundayDaytime; }
    public Integer getSundayNight() { return sundayNight; }
    public void setSundayNight(Integer sundayNight) { this.sundayNight = sundayNight; }
    public BigDecimal getWeekdayWorkHours() { return weekdayWorkHours; }
    public void setWeekdayWorkHours(BigDecimal weekdayWorkHours) { this.weekdayWorkHours = weekdayWorkHours; }
    public BigDecimal getSaturdayWorkHours() { return saturdayWorkHours; }
    public void setSaturdayWorkHours(BigDecimal saturdayWorkHours) { this.saturdayWorkHours = saturdayWorkHours; }
    public BigDecimal getSundayWorkHours() { return sundayWorkHours; }
    public void setSundayWorkHours(BigDecimal sundayWorkHours) { this.sundayWorkHours = sundayWorkHours; }
    public BigDecimal getMonthlyWorkHours() { return monthlyWorkHours; }
    public void setMonthlyWorkHours(BigDecimal monthlyWorkHours) { this.monthlyWorkHours = monthlyWorkHours; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getWorkHistory() { return workHistory; }
    public void setWorkHistory(String workHistory) { this.workHistory = workHistory; }
    public String getAcademicBackground() { return academicBackground; }
    public void setAcademicBackground(String academicBackground) { this.academicBackground = academicBackground; }
    public String getSelfIntroduction() { return selfIntroduction; }
    public void setSelfIntroduction(String selfIntroduction) { this.selfIntroduction = selfIntroduction; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Timestamp getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Timestamp deletedAt) { this.deletedAt = deletedAt; }
}
