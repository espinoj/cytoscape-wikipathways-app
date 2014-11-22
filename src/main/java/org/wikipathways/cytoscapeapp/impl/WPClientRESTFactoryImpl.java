package org.wikipathways.cytoscapeapp.impl;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPClientFactory;
import org.cytoscape.application.CyApplicationConfiguration;


public class WPClientRESTFactoryImpl implements WPClientFactory {
  final CyApplicationConfiguration appConf;
  public WPClientRESTFactoryImpl(final CyApplicationConfiguration appConf) {
    this.appConf = appConf;
  }
  public WPClient create() {
    return new WPClientRESTImpl(appConf);
  }
}
