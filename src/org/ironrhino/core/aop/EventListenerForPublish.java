package org.ironrhino.core.aop;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
public class EventListenerForPublish implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

	private static final long serialVersionUID = 9062685218966998574L;

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		if (event.getEntity().getClass().isAnnotationPresent(PublishAware.class))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		if (event.getEntity().getClass().isAnnotationPresent(PublishAware.class))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		if (event.getEntity().getClass().isAnnotationPresent(PublishAware.class))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return true;
	}
}