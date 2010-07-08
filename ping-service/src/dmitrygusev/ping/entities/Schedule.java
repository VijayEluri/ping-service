package dmitrygusev.ping.entities;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Schedule implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6607545799866087941L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable=false)
    private String name;
    
    @Basic
    private List<Job> jobs;
    
    @Transient
    private boolean sorted;
    
    public Schedule() {
    }
    
    public void addJob(Job job) {
        if (! jobs.contains(job)) {
            jobs.add(job);
            sorted = false;
        }
    }
    public void removeJob(Job job) {
        jobs.remove(job);
    }
    public void updateJob(Job job) {
        if (jobs.contains(job))  {
            jobs.remove(job);
            jobs.add(job);
            sorted = false;
        }
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
        if (!sorted) {
            Collections.sort(jobs, new Comparator<Job>() {
                @Override
                public int compare(Job o1, Job o2) {
                    return o1.getTitleFriendly().compareTo(o2.getTitleFriendly());
                }
            });
            sorted = true;
        }
        return Collections.unmodifiableList(jobs);
    }
}
