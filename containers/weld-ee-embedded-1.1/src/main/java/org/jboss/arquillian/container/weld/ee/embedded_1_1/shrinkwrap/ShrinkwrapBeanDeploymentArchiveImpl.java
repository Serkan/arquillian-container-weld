/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.weld.ee.embedded_1_1.shrinkwrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EnterpriseBean;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.jboss.arquillian.container.weld.ee.embedded_1_1.mock.MockEjbDescriptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.classloader.ShrinkWrapClassLoader;
import org.jboss.shrinkwrap.impl.base.AssignableBase;
import org.jboss.shrinkwrap.impl.base.Validate;
import org.jboss.shrinkwrap.impl.base.asset.ArchiveAsset;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;

/**
 * ShrinkwrapBeanDeploymentArchiveImpl
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ShrinkwrapBeanDeploymentArchiveImpl extends AssignableBase implements ShrinkwrapBeanDeploymentArchive 
{
   private Archive<?> archive;
   
   private ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
   
   private ShrinkWrapClassLoader classLoader;
   
   public ShrinkwrapBeanDeploymentArchiveImpl(Archive<?> archive)
   {
      Validate.notNull(archive, "Archive must be specified");
      this.archive = archive;
      
      this.classLoader = new ShrinkWrapClassLoader(archive.getClass().getClassLoader(), archive);
   }

   @Override
   protected Archive<?> getArchive()
   {
      return archive;
   }

   public ShrinkWrapClassLoader getClassLoader()
   {
      return classLoader;
   }

   public String getId()
   {
      return archive.getName();
   }

   public ServiceRegistry getServices()
   {
      return serviceRegistry;
   }
   
   public Collection<URL> getBeansXml()
   {
      List<URL> beanClasses = new ArrayList<URL>();
      Map<ArchivePath, Node> nestedArchives = archive.getContent(Filters.include(".*\\.jar|.*\\.war"));
      for(final Map.Entry<ArchivePath, Node> nestedArchiveEntry : nestedArchives.entrySet())
      {
         if( !(nestedArchiveEntry.getValue().getAsset() instanceof ArchiveAsset))
         {
            continue;
         }
         ArchiveAsset nestedArchive = (ArchiveAsset)nestedArchiveEntry.getValue().getAsset();
         Map<ArchivePath, Node> classes = nestedArchive.getArchive().getContent(Filters.include(".*/beans.xml"));
         for(final Map.Entry<ArchivePath, Node> entry : classes.entrySet()) 
         {
            try 
            {
               beanClasses.add(
                     new URL(null, "archive://" + entry.getKey().get(), new URLStreamHandler() 
                     {
                        @Override
                        protected java.net.URLConnection openConnection(URL u) throws java.io.IOException 
                        {
                           return new URLConnection(u)
                           {
                              @Override
                              public void connect() throws IOException { }
                              
                              @Override
                              public InputStream getInputStream()
                                    throws IOException
                              {
                                 return entry.getValue().getAsset().openStream();
                              }
                           };
                        };
                     }));
            } 
            catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
      Map<ArchivePath, Node> classes = archive.getContent(Filters.include(".*/beans.xml"));
      for(final Map.Entry<ArchivePath, Node> entry : classes.entrySet()) 
      {
         try 
         {
            beanClasses.add(
                  new URL(null, "archive://" + entry.getKey().get(), new URLStreamHandler() 
                  {
                     @Override
                     protected java.net.URLConnection openConnection(URL u) throws java.io.IOException 
                     {
                        return new URLConnection(u)
                        {
                           @Override
                           public void connect() throws IOException { }
                           
                           @Override
                           public InputStream getInputStream()
                                 throws IOException
                           {
                              return entry.getValue().getAsset().openStream();
                           }
                        };
                     };
                  }));
         } 
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      return beanClasses;
   }

   public Collection<Class<?>> getBeanClasses()
   {
      List<Class<?>> beanClasses = new ArrayList<Class<?>>();

      try
      {
         Map<ArchivePath, Node> nestedArchives = archive.getContent(Filters.include(".*\\.jar|.*\\.war|.*\\.rar"));
         for(final Map.Entry<ArchivePath, Node> nestedArchiveEntry : nestedArchives.entrySet())
         {
            if( !(nestedArchiveEntry.getValue().getAsset() instanceof ArchiveAsset))
            {
               continue;
            }
            ArchiveAsset nestedArchive = (ArchiveAsset)nestedArchiveEntry.getValue().getAsset();
            Map<ArchivePath, Node> classes = nestedArchive.getArchive().getContent(Filters.include(".*\\.class"));
            for(Map.Entry<ArchivePath, Node> classEntry : classes.entrySet()) 
            {
               Class<?> loadedClass = getClassLoader().loadClass(
                     getClassName(classEntry.getKey())); 
   
               beanClasses.add(loadedClass);
            }
         }
         Map<ArchivePath, Node> classes = archive.getContent(Filters.include(".*\\.class"));
         for(Map.Entry<ArchivePath, Node> classEntry : classes.entrySet()) 
         {
            Class<?> loadedClass = getClassLoader().loadClass(
                  getClassName(classEntry.getKey())); 
   
            beanClasses.add(loadedClass);
         }
      }
      catch (ClassNotFoundException e) 
      {
         throw new RuntimeException("Could not load class from archive " + archive.getName(), e);
      }
      return beanClasses;
   }

   public Collection<BeanDeploymentArchive> getBeanDeploymentArchives()
   {
      return Collections.emptySet();
   }

   public Collection<EjbDescriptor<?>> getEjbs()
   {
      List<EjbDescriptor<?>> ejbs = new ArrayList<EjbDescriptor<?>>();
      for (Class<?> ejbClass : discoverEjbs(getBeanClasses()))
      {
         ejbs.add(MockEjbDescriptor.of(ejbClass));
      }
      return ejbs;
   }

   protected static Iterable<Class<?>> discoverEjbs(Iterable<Class<?>> webBeanClasses)
   {
      Set<Class<?>> ejbs = new HashSet<Class<?>>();
      for (Class<?> clazz : webBeanClasses)
      {
         if (clazz.isAnnotationPresent(Stateless.class) || clazz.isAnnotationPresent(Stateful.class) || clazz.isAnnotationPresent(MessageDriven.class) || clazz.isAnnotationPresent(Singleton.class) || EnterpriseBean.class.isAssignableFrom(clazz)) 
         {
            ejbs.add(clazz);
         }
      }
      return ejbs;
   }
   
   /*
    *  input:  /org/MyClass.class
    *  output: org.MyClass
    */
   public String getClassName(ArchivePath path)
   {
      String className = path.get();
      className = className.replaceAll("/WEB-INF/classes/", "");
      if(className.charAt(0) == '/')
      {
         className = className.substring(1);
      }
      className = className.replaceAll("\\.class", "");
      className = className.replaceAll("/", ".");
      return className;
   }
}