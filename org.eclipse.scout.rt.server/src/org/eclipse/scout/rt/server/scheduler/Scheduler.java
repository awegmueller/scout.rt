/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import javax.security.auth.Subject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.scout.commons.StoppableThread;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.ServerJob;
import org.eclipse.scout.rt.server.services.common.session.IServerSessionRegistryService;
import org.eclipse.scout.service.SERVICES;

public class Scheduler implements IScheduler {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(Scheduler.class);

  // dispatcher
  private P_Dispatcher m_dispatcher;
  // queue
  private Object m_queueLock;
  private HashSet<ISchedulerJob> m_availableJobs;
  private HashSet<ISchedulerJob> m_runningJobs;
  // timer
  private Ticker m_ticker;
  private boolean m_active = true;
  //
  private Subject m_subject;
  private IServerSession m_serverSession;

  public Scheduler(Subject subject, Class<? extends IServerSession> serverSessionType) throws ProcessingException {
    this(subject, serverSessionType, new Ticker(Calendar.MINUTE));
  }

  public Scheduler(Subject subject, Class<? extends IServerSession> serverSessionType, Ticker ticker) throws ProcessingException {
    m_availableJobs = new HashSet<ISchedulerJob>();
    m_runningJobs = new HashSet<ISchedulerJob>();
    m_queueLock = new Object();
    m_ticker = ticker;
    m_subject = subject;
    m_serverSession = SERVICES.getService(IServerSessionRegistryService.class).newServerSession(serverSessionType, subject);
  }

  @Override
  public void setActive(boolean b) {
    m_active = b;
  }

  @Override
  public boolean isActive() {
    return m_active;
  }

  @Override
  public Ticker getTicker() {
    return m_ticker;
  }

  @Override
  public void start() {
    synchronized (m_queueLock) {
      if (m_dispatcher == null) {
        m_dispatcher = new P_Dispatcher();
        m_dispatcher.start();
      }
    }
  }

  @Override
  public void stop() {
    synchronized (m_queueLock) {
      if (m_dispatcher != null) {
        m_dispatcher.setStopSignal();
        m_dispatcher = null;
        for (ISchedulerJob job : m_runningJobs) {
          try {
            job.setInterrupted(true);
          }
          catch (Throwable t) {
            LOG.error("" + job, t);
          }
        }
      }
    }
  }

  /**
   * Job Queue
   */

  private boolean matches(ISchedulerJob job, String groupId, String jobId) {
    return (groupId == null || groupId.equals(job.getGroupId())) && (jobId == null || jobId.equals(job.getJobId()));
  }

  @Override
  public void addJob(ISchedulerJob newJob) {
    if (newJob == null) {
      throw new IllegalArgumentException("job must not be null");
    }
    synchronized (m_queueLock) {
      newJob.setDisposed(false);
      String groupId = newJob.getGroupId();
      String jobId = newJob.getJobId();
      ArrayList<ISchedulerJob> oldJobs = new ArrayList<ISchedulerJob>();
      for (ISchedulerJob job : m_availableJobs) {
        if (matches(job, groupId, jobId)) {
          job.setDisposed(true);
          oldJobs.add(job);
        }
      }
      m_availableJobs.removeAll(oldJobs);
      m_availableJobs.add(newJob);
      // check if job should already be run
      boolean oldJobsRunning = false;
      for (ISchedulerJob job : m_runningJobs) {
        if (matches(job, groupId, jobId)) {
          oldJobsRunning = true;
          break;
        }
      }
      if (!oldJobsRunning) {
        TickSignal tick = m_ticker.getCurrentTick();
        visitJobWithoutLocking(newJob, tick);
      }
    }
  }

  /**
   * convenience for removeJobs(null,null)
   */
  @Override
  public void removeAllJobs() {
    removeJobs(null, null);
  }

  /**
   * @param groupId
   *          filter value or null as wildcard
   * @param jobId
   *          filter value or null as wildcard
   * @return the list of removed jobs
   */
  @Override
  public Collection<ISchedulerJob> removeJobs(String groupId, String jobId) {
    synchronized (m_queueLock) {
      ArrayList<ISchedulerJob> removedJobs = new ArrayList<ISchedulerJob>();
      for (ISchedulerJob job : m_availableJobs) {
        if (matches(job, groupId, jobId)) {
          job.setDisposed(true);
          removedJobs.add(job);
        }
      }
      m_availableJobs.removeAll(removedJobs);
      return removedJobs;
    }
  }

  /**
   * convenience for interruptJobs(null,null)
   */
  @Override
  public void interruptAllJobs() {
    interruptJobs(null, null);
  }

  /**
   * @param groupId
   *          filter value or null as wildcard
   * @param jobId
   *          filter value or null as wildcard
   * @return the list of interrupted jobs
   */
  @Override
  public Collection<ISchedulerJob> interruptJobs(String groupId, String jobId) {
    synchronized (m_queueLock) {
      ArrayList<ISchedulerJob> intJobs = new ArrayList<ISchedulerJob>();
      for (ISchedulerJob job : m_availableJobs) {
        if (matches(job, groupId, jobId)) {
          if (m_runningJobs.contains(job)) {
            job.setInterrupted(true);
            intJobs.add(job);
          }
        }
      }
      return intJobs;
    }
  }

  @Override
  public int getJobCount() {
    synchronized (m_queueLock) {
      return m_availableJobs.size();
    }
  }

  @Override
  public int getRunningJobCount() {
    synchronized (m_queueLock) {
      return m_runningJobs.size();
    }
  }

  /**
   * convenience for getJobs(null,jobId) Note that this will return the first
   * found job with that id even though there might be other jobs with that same
   * id
   */
  @Override
  public ISchedulerJob getJob(String jobId) {
    Collection<ISchedulerJob> list = getJobs(null, jobId);
    if (list.size() >= 1) {
      return list.iterator().next();
    }
    else {
      return null;
    }
  }

  /**
   * convenience for getJobs(null,null)
   */
  @Override
  public Collection<ISchedulerJob> getAllJobs() {
    return getJobs(null, null);
  }

  @Override
  public Collection<ISchedulerJob> getJobs(String groupId, String jobId) {
    synchronized (m_queueLock) {
      ArrayList<ISchedulerJob> jobs = new ArrayList<ISchedulerJob>();
      for (ISchedulerJob job : m_availableJobs) {
        if (matches(job, groupId, jobId)) {
          jobs.add(job);
        }
      }
      return jobs;
    }
  }

  /**
   * convenience for getRunningJobs(null,null)
   */
  @Override
  public Collection<ISchedulerJob> getAllRunningJobs() {
    return getRunningJobs(null, null);
  }

  @Override
  public Collection<ISchedulerJob> getRunningJobs(String groupId, String jobId) {
    synchronized (m_queueLock) {
      ArrayList<ISchedulerJob> jobs = new ArrayList<ISchedulerJob>();
      for (ISchedulerJob job : m_availableJobs) {
        if (matches(job, groupId, jobId)) {
          jobs.add(job);
        }
      }
      return jobs;
    }
  }

  protected void visitAllJobs(TickSignal tick) {
    synchronized (m_queueLock) {
      visitAllJobsWithoutLocking(tick);
    }
  }

  protected void visitAllJobsWithoutLocking(TickSignal tick) {
    for (ISchedulerJob job : new ArrayList<ISchedulerJob>(m_availableJobs)) {
      visitJobWithoutLocking(job, tick);
    }
  }

  protected void visitJob(ISchedulerJob job, TickSignal tick) {
    synchronized (m_queueLock) {
      visitJobWithoutLocking(job, tick);
    }
  }

  protected void visitJobWithoutLocking(ISchedulerJob job, TickSignal tick) {
    try {
      if (m_runningJobs.contains(job)) {
        // still running
        if (LOG.isInfoEnabled()) {
          if (job.acceptTick(tick)) {
            LOG.info("job " + job + " is still running at " + tick);
          }
        }
      }
      else {
        // idle
        if (job.isDisposed()) {
          m_availableJobs.remove(job);
        }
        else if (job.acceptTick(tick)) {
          m_runningJobs.add(job);
          if (LOG.isInfoEnabled()) {
            LOG.info("job " + job + " triggered at " + tick);
          }
          P_JobRunner runner = new P_JobRunner(job, tick);
          Thread t = new Thread(runner, "Scheduler.JobLauncher." + job.getGroupId() + "." + job.getJobId());
          t.setDaemon(true);
          t.start();
        }
      }
    }
    catch (Throwable t) {
      LOG.error("" + job, t);
    }
  }

  @Override
  public void handleJobExecution(final ISchedulerJob job, final TickSignal signal) throws ProcessingException {
    ServerJob serverJob = new ServerJob("Scheduler", m_serverSession, m_subject) {
      @Override
      protected IStatus runTransaction(IProgressMonitor monitor) throws Exception {
        job.run(Scheduler.this, signal);
        return Status.OK_STATUS;
      }
    };
    serverJob.schedule();
    try {
      serverJob.join();
    }
    catch (InterruptedException ie) {
      throw new ProcessingException("Interrupted", ie);
    }
  }

  /**
   * Every job trigger is launched using this private class
   */
  private class P_JobRunner implements Runnable {
    private ISchedulerJob m_job;
    private TickSignal m_signal;

    public P_JobRunner(ISchedulerJob job, TickSignal signal) {
      m_job = job;
      m_signal = signal;
    }

    public ISchedulerJob getJob() {
      return m_job;
    }

    public TickSignal getTickSignal() {
      return m_signal;
    }

    @Override
    public void run() {
      try {
        m_job.setInterrupted(false);
        handleJobExecution(m_job, m_signal);
      }
      catch (Throwable t) {
        LOG.error("uncaught exception", t);
      }
      finally {
        // remove job from running queue
        synchronized (m_queueLock) {
          m_runningJobs.remove(m_job);
          if (m_job.isDisposed()) {
            m_availableJobs.remove(m_job);
          }
        }
      }
    }
  }// end private class

  private class P_Dispatcher extends StoppableThread {

    public P_Dispatcher() {
      setName("Scheduler.Dispatcher");
      setDaemon(true);
    }

    @Override
    public void run() {
      if (LOG.isInfoEnabled()) {
        LOG.info("scheduler started");
      }
      while (!isStopSignal()) {
        try {
          if (isActive()) {
            TickSignal signal = m_ticker.waitForNextTick();
            if (LOG.isDebugEnabled()) {
              LOG.debug("tick " + signal);
            }
            visitAllJobs(signal);
          }
          else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("ticking suspended");
            }
            try {
              sleep(1000);
            }
            catch (InterruptedException ie) {
            }
          }
        }
        catch (Throwable t) {
          t.printStackTrace();
          LOG.error("unexpected error: ", t);
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("scheduler stopped");
      }
    }
  }

}
