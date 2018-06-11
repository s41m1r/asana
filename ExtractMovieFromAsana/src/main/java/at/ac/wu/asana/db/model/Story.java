package at.ac.wu.asana.db.model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Story {

	@Id
	public String id;

	public Date date;
	public Time time;

	public String user;

	public String type;
	
	@Column(columnDefinition="TEXT")
	public String text;

	public String action;

	@ManyToOne
	public Task task;

	public Timestamp timestamp;

	@Override
	public String toString() {
		return "Story [id=" + id + ", type=" + type + ", text=" + text + ", action=" + action + ", user=" + user
				+ ", timestamp=" + timestamp + "]";
	}


}
