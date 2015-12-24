package moni.avl03.state;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class JmsManager {
	private static final Logger logger = LoggerFactory.getLogger(JmsManager.class);
	private DefaultMessageListenerContainer jmsContainer;
	private ChannelKeeper channelKeeper;
	private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	private boolean stoped = false;
	private Object sync = new Object();

	public void setJmsContainer(DefaultMessageListenerContainer jmsContainer) {
		this.jmsContainer = jmsContainer;
	}

	public void setChannelKeeper(ChannelKeeper channelKeeper) {
		this.channelKeeper = channelKeeper;
	}

	public void init() {
		taskScheduler.initialize();
	}

	public void stopJms() {
		synchronized (sync) {
			if (!stoped) {
				stoped = true;
				jmsContainer.stop();
				logger.info("infoJmsContainer stoped.");

				startScheduledConnectCheck();
			} else {
				logger.warn("try stop jms, but it stoped before.");
			}
		}
	}

	private void startJms() {
		synchronized (sync) {
			if (stoped) {
				stoped = false;
				jmsContainer.start();
				logger.info("infoJmsContainer started.");
			} else {
				logger.error("try start jms, but it started.");
			}
		}
	}

	private void startScheduledConnectCheck() {
		Date d = new Date(new Date().getTime() + (60 * 1000));
		taskScheduler.schedule(this::checkConnect, d);
	}

	private void checkConnect() {
		boolean canConnect = false;
		try {
			canConnect = channelKeeper.checkConnect();
		} catch (Exception e) {
			logger.error("Check connect ERROR.", e);
		}

		if (canConnect) {
			startJms();
			logger.info("can connect to wialon. stop check connection and start jms.");
		} else {
			startScheduledConnectCheck();
			logger.warn("cannot connect to wialon. next check throw little interval.");
		}
	}
}
