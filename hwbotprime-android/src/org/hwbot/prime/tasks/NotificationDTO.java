package org.hwbot.prime.tasks;

public class NotificationDTO {

	private String id;
	private Integer userId;
	private String user;
	private Integer teamId;
	private String team;
	private Integer libraryItemId;
	private Integer resultId;
	private Integer contestId;
	private String contest;
	private Integer manufacturerId;
	private Long date;
	private int type;
	private String message;
	private String link;
	private int votes;
	private String parentNotificationId;
	private String image;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getTeamId() {
		return teamId;
	}

	public void setTeamId(Integer teamId) {
		this.teamId = teamId;
	}

	public Integer getLibraryItemId() {
		return libraryItemId;
	}

	public void setLibraryItemId(Integer libraryItemId) {
		this.libraryItemId = libraryItemId;
	}

	public Integer getResultId() {
		return resultId;
	}

	public void setResultId(Integer resultId) {
		this.resultId = resultId;
	}

	public Integer getContestId() {
		return contestId;
	}

	public void setContestId(Integer contestId) {
		this.contestId = contestId;
	}

	public Integer getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(Integer manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public int getVotes() {
		return votes;
	}

	public void setVotes(int votes) {
		this.votes = votes;
	}

	public String getParentNotificationId() {
		return parentNotificationId;
	}

	public void setParentNotificationId(String parentNotificationId) {
		this.parentNotificationId = parentNotificationId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getContest() {
		return contest;
	}

	public void setContest(String contest) {
		this.contest = contest;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public String toString() {
		return "NotificationDTO [" + (id != null ? "id=" + id + ", " : "") + (userId != null ? "userId=" + userId + ", " : "")
				+ (teamId != null ? "teamId=" + teamId + ", " : "") + (libraryItemId != null ? "libraryItemId=" + libraryItemId + ", " : "")
				+ (resultId != null ? "resultId=" + resultId + ", " : "") + (contestId != null ? "contestId=" + contestId + ", " : "")
				+ (manufacturerId != null ? "manufacturerId=" + manufacturerId + ", " : "") + (date != null ? "date=" + date + ", " : "") + "type=" + type
				+ ", " + (message != null ? "message=" + message + ", " : "") + (link != null ? "link=" + link + ", " : "") + "votes=" + votes + ", "
				+ (parentNotificationId != null ? "parentNotificationId=" + parentNotificationId : "") + "]";
	}

}
