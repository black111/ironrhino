package org.ironrhino.batch;

import org.ironrhino.core.remoting.Remoting;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.launch.NoSuchJobException;

@Remoting
public interface JobStepExecutor {

	public void execute(Long jobExecutionId, Long stepExecutionId, String stepName)
			throws JobInterruptedException, NoSuchJobException;

}
