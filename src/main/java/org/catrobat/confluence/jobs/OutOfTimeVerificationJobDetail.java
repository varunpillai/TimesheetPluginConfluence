package org.catrobat.confluence.jobs;

import com.atlassian.confluence.user.UserAccessor;
import org.catrobat.confluence.services.MailService;
import org.catrobat.confluence.services.TimesheetService;
import org.quartz.JobDetail;

/**
 * This class allows Spring dependencies to be injected into {@link ActivityVerificationJob}.
 * A bug in Confluence's auto-wiring prevents  Job components from being auto-wired.
 */
public class OutOfTimeVerificationJobDetail extends JobDetail {
    private final TimesheetService ts;
    private final UserAccessor ua;
    private final MailService ms;

    public OutOfTimeVerificationJobDetail(TimesheetService ts, UserAccessor ua, MailService ms) {
        super();
        this.ts = ts;
        this.ua = ua;
        this.ms = ms;

        setName(OutOfTimeVerificationJobDetail.class.getSimpleName());
        setJobClass(OutOfTimeVerificationJob.class);
    }

    public UserAccessor getUserAccessor() {
        return ua;
    }

    public TimesheetService getTimesheetService() {
        return ts;
    }

    public MailService getMailService() {
        return ms;
    }
}
