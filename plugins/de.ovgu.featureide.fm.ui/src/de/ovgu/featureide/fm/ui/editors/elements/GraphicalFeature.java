/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.elements;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent.EventType;
import de.ovgu.featureide.fm.core.base.event.IEventListener;
import de.ovgu.featureide.fm.ui.editors.FeatureConnection;
import de.ovgu.featureide.fm.ui.editors.FeatureUIHelper;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeature;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeatureModel;

/**
 * Graphical representation of an {@link IFeature} instance.
 * 
 * @author Sebastian Krieter
 * 
 */
public class GraphicalFeature implements IGraphicalFeature {

	protected final FeatureConnection sourceConnection;

	protected boolean constraintSelected;

	protected IFeature feature;

	protected final IGraphicalFeatureModel graphicalFeatureModel;

	protected Point location = new Point(0, 0);

	protected Dimension dimension = new Dimension(10, 10);

	private IEventListener uiObject;

	public GraphicalFeature(IFeature correspondingFeature, IGraphicalFeatureModel graphicalFeatureModel) {
		this.graphicalFeatureModel = graphicalFeatureModel;
		this.feature = correspondingFeature;
		sourceConnection = new FeatureConnection(this);
	}

	public GraphicalFeature(GraphicalFeature graphicalFeature) {
		constraintSelected = graphicalFeature.constraintSelected;
		location = graphicalFeature.location;
		dimension = graphicalFeature.dimension;
		feature = graphicalFeature.feature;
		graphicalFeatureModel = graphicalFeature.graphicalFeatureModel;
		sourceConnection = graphicalFeature.sourceConnection;
	}

	@Override
	public IFeature getObject() {
		return feature;
	}

	@Override
	public GraphicItem getItemType() {
		return GraphicItem.Feature;
	}

	@Override
	public boolean isConstraintSelected() {
		return constraintSelected;
	}

	@Override
	public void setConstraintSelected(boolean selection) {
		constraintSelected = selection;
	}

	@Override
	public Point getLocation() {
		return location;
	}

	@Override
	public void setLocation(Point newLocation) {
		if (!location.equals(newLocation)) {
			location = newLocation;
			update(FeatureIDEEvent.getDefault(EventType.LOCATION_CHANGED));
		}
	}

	@Override
	public Dimension getSize() {
		return dimension;
	}

	@Override
	public void setSize(Dimension size) {
		this.dimension = size;
	}

	@Override
	public IGraphicalFeatureModel getGraphicalModel() {
		return graphicalFeatureModel;
	}

	@Override
	public void addTargetConnection(FeatureConnection connection) {

	}

	@Override
	public FeatureConnection getSourceConnection() {
		if (feature.getStructure().isRoot()) {
			return null;
		}
		sourceConnection.setTarget(FeatureUIHelper.getGraphicalParent(feature, graphicalFeatureModel));
		return sourceConnection;
	}

	@Override
	public List<FeatureConnection> getSourceConnectionAsList() {
		final List<FeatureConnection> list;
		if (feature.getStructure().isRoot()) {
			list = new LinkedList<>();
		} else {
			list = new LinkedList<>();
			list.add(getSourceConnection());
		}
		return (list);
	}
	
	@Override
	public List<FeatureConnection> getTargetConnections() {
		final List<FeatureConnection> targetConnections = new LinkedList<>();
		for (IFeatureStructure child : feature.getStructure().getChildren()) {
			targetConnections.add(FeatureUIHelper.getGraphicalFeature(child, graphicalFeatureModel).getSourceConnection());
		}
		return targetConnections;
	}

	@Override
	public String toString() {
		return feature.toString();
	}

	@Override
	public String getGraphicType() {
		return "";
	}

	public GraphicalFeature clone() {
		return new GraphicalFeature(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((feature == null) ? 0 : feature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof GraphicalFeature)) {
			return false;
		}
		GraphicalFeature other = (GraphicalFeature) obj;
		if (feature == null) {
			if (other.feature != null) {
				return false;
			}
		} else if (!feature.equals(other.feature)) {
			return false;
		}
		return true;
	}
	
	@Override
	public void update(FeatureIDEEvent event) {
		if (uiObject != null) {
			uiObject.propertyChange(event);
		}
	}

	@Override
	public void registerUIObject(IEventListener listener) {
		this.uiObject = listener;
	}

}