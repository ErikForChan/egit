/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;

/**
 * Label decorator for warning/error problem markers (used in Staging View and
 * Commit Dialog).
 * <p>
 * Users must make the decoratable element implement {@link IProblemDecoratable}.
 */
public class ProblemLabelDecorator extends BaseLabelProvider implements
		ILabelDecorator, IResourceChangeListener {

	private final StructuredViewer viewer;

	private final ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	/**
	 * @param viewer
	 *            the viewer to use for label updates because of changed
	 *            resources, or null for none
	 */
	public ProblemLabelDecorator(StructuredViewer viewer) {
		this.viewer = viewer;
		if (this.viewer != null)
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		resourceManager.dispose();
		if (this.viewer != null)
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		IProblemDecoratable decoratable = getProblemDecoratable(element);
		if (decoratable != null) {
			int problemSeverity = decoratable.getProblemSeverity();
			if (problemSeverity == IMarker.SEVERITY_ERROR)
				return getDecoratedImage(image, ISharedImages.IMG_ERROR_OVR);
			else if (problemSeverity == IMarker.SEVERITY_WARNING)
				return getDecoratedImage(image, ISharedImages.IMG_WARNING_OVR);
		}
		return null;
	}

	@Override
	public String decorateText(String text, Object element) {
		// No decoration
		return null;
	}

	private IProblemDecoratable getProblemDecoratable(Object element) {
		if (element instanceof IProblemDecoratable)
			return (IProblemDecoratable) element;
		else
			return null;
	}

	private Image getDecoratedImage(Image base, String teamImageId) {
		ImageDescriptor overlay = TeamImages.getImageDescriptor(teamImageId);
		DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
				overlay, IDecoration.BOTTOM_LEFT);
		return (Image) this.resourceManager.get(decorated);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		Set<IResource> resources = new HashSet<>();

		IMarkerDelta[] markerDeltas = event.findMarkerDeltas(IMarker.PROBLEM,
				true);
		for (IMarkerDelta delta : markerDeltas) {
			// Also add parents
			IResource resource = delta.getResource();
			while (resource.getType() != IResource.ROOT
					&& resources.add(resource)) {
				resource = resource.getParent();
			}
		}

		if (!resources.isEmpty()) {
		    updateLabels(resources);
		}
	}

	private void updateLabels(Set<IResource> changedResources) {
		List<Object> elements = getAffectedElements(changedResources);
		if (!elements.isEmpty()) {
			final Object[] updateElements = elements.toArray(new Object[elements.size()]);
			Display display = viewer.getControl().getDisplay();
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					fireLabelProviderChanged(new LabelProviderChangedEvent(
							ProblemLabelDecorator.this, updateElements));
				}
			});
		}
	}

	private List<Object> getAffectedElements(Set<IResource> resources) {
		List<Object> result = new ArrayList<>();
		if (viewer.getContentProvider() instanceof IStructuredContentProvider) {
			IStructuredContentProvider contentProvider = (IStructuredContentProvider) viewer.getContentProvider();
			getAffectedElements(resources, contentProvider.getElements(null),
					contentProvider, result);
		}
		return result;
	}

	private void getAffectedElements(Set<IResource> resources,
			Object[] elements, IStructuredContentProvider contentProvider,
			List<Object> result) {
		for (Object element : elements) {
			IResource resource = AdapterUtils.adapt(element, IResource.class);
			if (resource != null && resources.contains(resource)) {
				result.add(element);
				if (contentProvider instanceof ITreeContentProvider) {
					getAffectedElements(resources,
							((ITreeContentProvider) contentProvider)
									.getChildren(element),
							contentProvider, result);
				}
			}
		}
	}
}
