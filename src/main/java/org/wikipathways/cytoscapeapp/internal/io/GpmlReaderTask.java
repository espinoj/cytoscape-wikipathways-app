// WikiPathways App for Cytoscape
//
// Copyright 2013-2014 WikiPathways
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wikipathways.cytoscapeapp.internal.io;

import java.io.IOException;
import java.io.InputStream;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.cytoscape.task.NetworkTaskFactory;
import org.pathvisio.core.model.Pathway;

/**
 * 
 * @author martinakutmon
 * Reads the GPML file and creates a GPMLNetwork 
 * TODO: currently network and pathway view are initialized --> setting!
 */
public class GpmlReaderTask extends AbstractTask implements CyNetworkReader {
    public static final String PATHWAY_DESC = "Pathway";
    public static final String NETWORK_DESC = "Network";

    final CyEventHelper eventHelper;
    final CyNetworkFactory netFactory;
    final CyNetworkManager netMgr;
    final CyNetworkViewFactory netViewFactory;
    final CyNetworkViewManager netViewMgr;
    final CyLayoutAlgorithmManager layoutMgr;
    final Annots annots;
    final GpmlVizStyle vizStyle;
    final NetworkTaskFactory showLODTF;
    final CyNetworkNaming netNaming;

	InputStream input = null;
    final String fileName;

    @Tunable(description="Import as:", groups={"WikiPathways"})
    public ListSingleSelection<String> importMethod = new ListSingleSelection<String>(PATHWAY_DESC, NETWORK_DESC);

	public GpmlReaderTask(
            final CyEventHelper eventHelper,
            final CyNetworkFactory netFactory,
            final CyNetworkManager netMgr,
            final CyNetworkViewFactory netViewFactory,
            final CyNetworkViewManager netViewMgr,
            final CyLayoutAlgorithmManager layoutMgr,
            final Annots annots,
            final GpmlVizStyle vizStyle,
            final NetworkTaskFactory showLODTF,
            final CyNetworkNaming netNaming,
            final InputStream input,
            final String fileName) {
        this.eventHelper = eventHelper;
        this.netFactory = netFactory;
        this.netMgr = netMgr;
        this.netViewFactory = netViewFactory;
        this.netViewMgr = netViewMgr;
        this.layoutMgr = layoutMgr;
        this.annots = annots;
        this.vizStyle = vizStyle;
        this.showLODTF = showLODTF;
        this.netNaming = netNaming;
        this.input = input;
        this.fileName = fileName;
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
        monitor.setTitle("Read GPML file " + fileName);
		monitor.setProgress(-1.0);

        monitor.setStatusMessage("Parsing GPML file");
        final Pathway pathway = new Pathway();
        pathway.readFromXml(input, true);
        input = null;
     
        monitor.setStatusMessage("Constructing network");
        final String name = pathway.getMappInfo().getMapInfoName();
        final CyNetworkView view = newNetwork(name);
        if(importMethod.getSelectedValue().equals(PATHWAY_DESC)) {
        	(new GpmlToPathway(eventHelper, annots, pathway, view)).convert();
        } else {
        	(new GpmlToNetwork(eventHelper, pathway, view)).convert();
        	CyLayoutAlgorithm layout = layoutMgr.getLayout("force-directed");
        	insertTasksAfterCurrentTask(layout.createTaskIterator(view, layout.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null));
        }
        insertTasksAfterCurrentTask(showLODTF.createTaskIterator(view.getModel()));
        insertTasksAfterCurrentTask(new AbstractTask() {
            public void run(TaskMonitor monitor) {
              updateNetworkView(view);
          }

          public void cancel() {}
      });
    }

    public void cancel() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {}
        }
    }

    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        return null;
    }

    public CyNetwork[] getNetworks() {
        return new CyNetwork[0];
    }

    private CyNetworkView newNetwork(final String name) {
        final CyNetwork net = netFactory.createNetwork();
        net.getRow(net).set(CyNetwork.NAME, netNaming.getSuggestedNetworkTitle(name));
        netMgr.addNetwork(net);
        final CyNetworkView view = netViewFactory.createNetworkView(net);
        netViewMgr.addNetworkView(view);
        return view;
    }

    private void updateNetworkView(final CyNetworkView netView) {
        vizStyle.apply(netView);
        netView.fitContent();
        netView.updateView();
    }
}
