package com.example.nss.analytics.application.port;

import com.example.nss.analytics.application.query.Disease;
import com.example.nss.analytics.application.query.EpidemiologyTotals;
import com.example.nss.analytics.application.query.MetadataEntry;
import com.example.nss.analytics.application.query.Municipality;
import com.example.nss.analytics.application.query.TerritoryAggregate;
import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import java.util.List;

public interface EpidemiologyQueryPort {
  List<TerritoryAggregate> aggregate(
      EpidemiologyFilter filter, Geography geography, List<String> territoryIds);

  EpidemiologyTotals totals(
      EpidemiologyFilter filter, Geography geography, List<String> territoryIds);

  List<Disease> diseases();

  List<MetadataEntry> metadata();

  List<Municipality> municipalities(String ufCode);
}
