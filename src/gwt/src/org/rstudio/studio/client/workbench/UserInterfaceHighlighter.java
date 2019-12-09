/*
 * UserInterfaceHighlighter.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandEvent;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.dom.DOMRect;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.events.HighlightEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserInterfaceHighlighter
      implements CommandHandler,
                 HighlightEvent.Handler
                 
{
   private static class HighlightPair
   {
      public HighlightPair(Element monitoredElement,
                           Element highlightedElement)
      {
         monitoredElement_ = monitoredElement;
         highlightedElement_ = highlightedElement;
      }

      public Element getMonitoredElement()
      {
         return monitoredElement_;
      }
      
      public Element getHighlightedElement()
      {
         return highlightedElement_;
      }
      
      private final Element monitoredElement_;
      private final Element highlightedElement_;
   }
   
   @Inject
   public UserInterfaceHighlighter(Commands commands,
                                   EventBus events)
   {
      events.addHandler(CommandEvent.TYPE, this);
      events.addHandler(HighlightEvent.TYPE, this);
      
      highlightPairs_ = new ArrayList<>();
      
      repositionTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            repositionHighlighters();
         }
      };
   }
   
   @Override
   public void onCommand(AppCommand command)
   {
      // TODO: Currently used for debugging + testing.
      for (String prefix : new String[] { ".rstudio_", ".rstudio_tb_" })
      {
         String query = prefix + command.getId().toLowerCase();
         highlight(query, 0);
      }
   }

   @Override
   public void onHighlight(HighlightEvent event)
   {
      String target = event.getData().getQuery();
      int parent = event.getData().getParent();
      highlight(target.toLowerCase(), parent);
   }
   
   private void highlight(String query, int parent)
   {
      removeHighlightElements();
      addHighlightElements(query, parent);
   }
   
   private void removeHighlightElements()
   {
      for (HighlightPair pair : highlightPairs_)
         pair.getHighlightedElement().removeFromParent();
      
      highlightPairs_.clear();
   }
   
   private void addHighlightElements(String query, int parent)
   {
      NodeList<Element> els = DomUtils.querySelectorAll(Document.get().getBody(), query);
      
      int max = Math.min(20, els.getLength());
      for (int i = 0; i < max; i++)
      {
         // retrieve the requested element (selecting a parent if so requested)
         Element el = els.getItem(i);
         for (int j = 0; j < parent; j++)
            el = el.getParentElement();
         
         // create highlight element
         Element highlightEl = Document.get().createDivElement();
         highlightEl.addClassName(RES.styles().highlightEl());
         Document.get().getBody().appendChild(highlightEl);
         
         // record the pair of elements
         highlightPairs_.add(new HighlightPair(el, highlightEl));
      }
      
      repositionHighlighters();
   }
   
   private void repositionHighlighters()
   {
      int scrollX = Window.getScrollLeft();
      int scrollY = Window.getScrollTop();
      
      final int borderPx = 0;
      
      for (HighlightPair pair : highlightPairs_)
      {
         Element monitoredEl = pair.getMonitoredElement();
         Element highlightEl = pair.getHighlightedElement();
         
         if (monitoredEl != null)
         {
            highlightEl.getStyle().setVisibility(Visibility.VISIBLE);
         }
         else
         {
            highlightEl.getStyle().setVisibility(Visibility.HIDDEN);
         }
         
         DOMRect bounds = ElementEx.getBoundingClientRect(monitoredEl);
         
         int top = scrollY + bounds.getTop() - borderPx;
         int left = scrollX + bounds.getLeft() - borderPx;
         int width = bounds.getWidth() + borderPx + borderPx;
         int height = bounds.getHeight() + borderPx + borderPx;
         
         // Ignore out-of-bounds elements.
         
         // Avoid using too-narrow highlights.
         if (width < 20)
         {
            int rest = 20 - width;
            left = left - (rest / 2);
            width = 20;
         }
         
         // This is a hack to give buttons with labels a bit more padding.
         if (width > height + 2)
            width = width + 2;
         
         highlightEl.getStyle().setTop(top, Unit.PX);
         highlightEl.getStyle().setLeft(left, Unit.PX);
         highlightEl.getStyle().setWidth(width, Unit.PX);
         highlightEl.getStyle().setHeight(height, Unit.PX);
      }
   }
   

   public interface Styles extends CssResource
   {
      String highlightEl();
   }

   public interface Resources extends ClientBundle
   {
      @Source("UserInterfaceHighlighter.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   private final List<HighlightPair> highlightPairs_;
   private final Timer repositionTimer_;

}