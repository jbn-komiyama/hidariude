package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * 秘書プロフィール（稼働条件・自由記述）。
 * availability: 0=不可 / 1=相談 / 2=可
 */
public class Profile implements Serializable{
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

    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

    /** --- getter / setter --- */
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSecretaryId() { return secretaryId; }
    public void setSecretaryId(UUID secretaryId) { this.secretaryId = secretaryId; }

    public Integer getWeekdayMorning() { return weekdayMorning; }
    public void setWeekdayMorning(Integer v) { this.weekdayMorning = v; }

    public Integer getWeekdayDaytime() { return weekdayDaytime; }
    public void setWeekdayDaytime(Integer v) { this.weekdayDaytime = v; }

    public Integer getWeekdayNight() { return weekdayNight; }
    public void setWeekdayNight(Integer v) { this.weekdayNight = v; }

    public Integer getSaturdayMorning() { return saturdayMorning; }
    public void setSaturdayMorning(Integer v) { this.saturdayMorning = v; }

    public Integer getSaturdayDaytime() { return saturdayDaytime; }
    public void setSaturdayDaytime(Integer v) { this.saturdayDaytime = v; }

    public Integer getSaturdayNight() { return saturdayNight; }
    public void setSaturdayNight(Integer v) { this.saturdayNight = v; }

    public Integer getSundayMorning() { return sundayMorning; }
    public void setSundayMorning(Integer v) { this.sundayMorning = v; }

    public Integer getSundayDaytime() { return sundayDaytime; }
    public void setSundayDaytime(Integer v) { this.sundayDaytime = v; }

    public Integer getSundayNight() { return sundayNight; }
    public void setSundayNight(Integer v) { this.sundayNight = v; }

    public BigDecimal getWeekdayWorkHours() { return weekdayWorkHours; }
    public void setWeekdayWorkHours(BigDecimal v) { this.weekdayWorkHours = v; }

    public BigDecimal getSaturdayWorkHours() { return saturdayWorkHours; }
    public void setSaturdayWorkHours(BigDecimal v) { this.saturdayWorkHours = v; }

    public BigDecimal getSundayWorkHours() { return sundayWorkHours; }
    public void setSundayWorkHours(BigDecimal v) { this.sundayWorkHours = v; }

    public BigDecimal getMonthlyWorkHours() { return monthlyWorkHours; }
    public void setMonthlyWorkHours(BigDecimal v) { this.monthlyWorkHours = v; }

    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }

    public String getQualification() { return qualification; }
    public void setQualification(String v) { this.qualification = v; }

    public String getWorkHistory() { return workHistory; }
    public void setWorkHistory(String v) { this.workHistory = v; }

    public String getAcademicBackground() { return academicBackground; }
    public void setAcademicBackground(String v) { this.academicBackground = v; }

    public String getSelfIntroduction() { return selfIntroduction; }
    public void setSelfIntroduction(String v) { this.selfIntroduction = v; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { this.createdAt = v; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date v) { this.updatedAt = v; }

    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date v) { this.deletedAt = v; }
}
