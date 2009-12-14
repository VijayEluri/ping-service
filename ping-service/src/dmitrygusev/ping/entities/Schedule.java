package dmitrygusev.ping.entities;

import java.util.Collections;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Schedule {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable=false)
	private String name;
	
	@Basic
	private List<Job> jobs;
	
	public Schedule() {
	}
	
	public void addJob(Job job) {
		if (! jobs.contains(job)) {
			jobs.add(job);
		}
	}
	public void removeJob(Job job) {
		jobs.remove(job);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getId() {
		return id;
	}
	public List<Job> getJobs() {
		return Collections.unmodifiableList(jobs);
	}
}
