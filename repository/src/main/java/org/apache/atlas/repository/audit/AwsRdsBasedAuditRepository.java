package org.apache.atlas.repository.audit;

import org.apache.atlas.AtlasException;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.annotation.ConditionalOnAtlasProperty;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

@Singleton
@Component
@ConditionalOnAtlasProperty(property = "atlas.EntityAuditRepository.impl")
public class AwsRdsBasedAuditRepository implements EntityAuditRepository {

  @Override
  public void putEventsV1(final EntityAuditEvent... events) throws AtlasException {

  }

  @Override
  public void putEventsV1(final List<EntityAuditEvent> events) throws AtlasException {

  }

  @Override
  public List<EntityAuditEvent> listEventsV1(final String entityId, final String startKey, final short n)
      throws AtlasException {
    return null;
  }

  @Override
  public void putEventsV2(final EntityAuditEventV2... events) throws AtlasBaseException {

  }

  @Override
  public void putEventsV2(final List<EntityAuditEventV2> events) throws AtlasBaseException {

  }

  @Override
  public List<EntityAuditEventV2> listEventsV2(final String entityId, final String startKey, final short n)
      throws AtlasBaseException {
    return null;
  }

  @Override
  public Set<String> getEntitiesWithTagChanges(final long fromTimestamp, final long toTimestamp)
      throws AtlasBaseException {
    return null;
  }

  @Override
  public List<Object> listEvents(final String entityId, final String startKey, final short n)
      throws AtlasBaseException {
    return null;
  }

  @Override
  public long repositoryMaxSize() {
    return 0;
  }

  @Override
  public List<String> getAuditExcludeAttributes(final String entityType) {
    return null;
  }

}
