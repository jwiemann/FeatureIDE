/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.conversion;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.prop4j.And;
import org.prop4j.AtLeast;
import org.prop4j.AtMost;
import org.prop4j.Choose;
import org.prop4j.Equals;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.NodeWriter;
import org.prop4j.Not;
import org.prop4j.Or;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;

/**
 * Converter using negation normal form
 * 
 * @author Alexander Kn�ppel
 */
public class NNFConverter implements IConverterStrategy {
	/* Feature model factory */
	protected static final IFeatureModelFactory factory = FMFactoryManager.getFactory();
	/* Working feature model */
	protected IFeatureModel fm;
	/* Preserving configuration semantics */
	protected boolean preserve = false;
	/* Running number for naming */
	private static int number = 0;
	
	/**
	 * Restructures root if needed
	 * @param fm Feature model
	 * @param name Name of new root
	 */
	protected void restructureRoot(String name) {
		if(!fm.getStructure().getRoot().isAnd()) {
			IFeature newRoot = factory.createFeature(fm, name);
			newRoot.getStructure().addChild(fm.getStructure().getRoot());
			newRoot.getStructure().setMandatory(true);
			fm.getStructure().setRoot(newRoot.getStructure());
		}
	}
	
	/**
	 * Adds a new element under root
	 * @param fm Feature model
	 * @param name Name of element
	 * @return The top element for further actions
	 */
	protected IFeature prepareTopElement(String name) {
		IFeature top = factory.createFeature(fm, name);
		fm.getStructure().getRoot().addChild(top.getStructure());
		top.getStructure().changeToAnd();
		top.getStructure().setAbstract(true);
		top.getStructure().setMandatory(true);
		
		return top;
	}	
	
	protected void addRequires(String f1, String f2) {
		Node requires = new Implies(new Literal(f1), new Literal(f2));
		fm.addConstraint(factory.createConstraint(fm, requires));
	}
	
	protected void addExcludes(String f1, String f2) {
		Node excludes = new Implies(new Literal(f1), new Not(new Literal(f2)));
		fm.addConstraint(factory.createConstraint(fm, excludes));
	}
	
	private void createStructureAndConstraints(IFeature top, List<Node> nodes, int level) {	
		for(Node node : nodes) {
			// Terminal feature
			if(node.getContainedFeatures().size() == 1) {
				String name = node.getContainedFeatures().get(0) + (number++);
				
				IFeature feature = factory.createFeature(top.getFeatureModel(), name);
				feature.getStructure().setAbstract(true);
				feature.getStructure().setMandatory(false);
				top.getStructure().addChild(feature.getStructure());
				
				if(!(node instanceof Not) && ((Literal)node).positive) {
					addRequires(name, node.getContainedFeatures().get(0));
					if(preserve) {
						addRequires(node.getContainedFeatures().get(0), name);
					}
				} else {
					addExcludes(name, node.getContainedFeatures().get(0));
				}
				
				continue;
			}
		
			// Non-Terminal feature: either And or Or
			IFeature feature = factory.createFeature(top.getFeatureModel(), "f" + (number++));
			feature.getStructure().setAbstract(true);
			feature.getStructure().setMandatory(true);
			if(node instanceof And) {
				feature.getStructure().setAnd();
			} else {
				feature.getStructure().setOr();
			}
			
			createStructureAndConstraints(feature, Arrays.asList(node.getChildren()), level+1);
			top.getStructure().addChild(feature.getStructure());
		}
	}
	
	protected void createStructureAndConstraints(IFeature top, List<Node> nodes) {	
		this.createStructureAndConstraints(top, nodes, 0);
	}
	
	@Override
	public IFeatureModel convert(IFeatureModel fm, List<Node> nodes, boolean preserve) {
		this.fm = fm.clone();
		this.preserve = preserve;
		restructureRoot("NewRoot");
		IFeature top = prepareTopElement("top");
		createStructureAndConstraints(top, nodes);
		return this.fm;
	}

	@Override
	public List<Node> preprocess(IConstraint constraint) {
		List<Node> elements = new LinkedList<Node>();
		Node node = constraint.getNode().clone();
		
		String[] supported = new String[] {"!", " && ", " || ", NodeWriter.noSymbol, NodeWriter.noSymbol, NodeWriter.noSymbol, 
				   NodeWriter.noSymbol, NodeWriter.noSymbol, NodeWriter.noSymbol};
		node = node.eliminateNotSupportedSymbols(supported);
		
		node = propagateNegation(node, false);
		
		elements.add(node);
		return elements;
	}
	
	private Node propagateNegation(Node node, boolean negated) {
		if(node instanceof Not) {
			negated = !negated;
			return propagateNegation(node.getChildren()[0], negated);
		} else if(node instanceof And || node instanceof Or) {
			List<Node> nodelist = new ArrayList<Node>();
			for(Node tmp : node.getChildren()) {
				nodelist.add(propagateNegation(tmp, negated));
			}
			
			if(node instanceof And) {
				if(negated) {
					return new Or((Object[]) nodelist.toArray());
				} else {
					return new And((Object[]) nodelist.toArray());
				}
			} else {
				if(negated) {
					return new And((Object[]) nodelist.toArray());
				} else {
					return new Or((Object[]) nodelist.toArray());
				}
			}
		}
		
		//node is an atom
		if(negated)
			return new Not(node);
		
		return node;
	}

}