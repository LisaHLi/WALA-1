/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class provides Instance Key call backs where each instance is in the same equivalence class as all other instances of the
 * same concrete type.
 * 
 * @author sfink
 */
public class ClassBasedInstanceKeys implements InstanceKeyFactory {

  private final static boolean DEBUG = false;

  private final AnalysisOptions options;

  private final IClassHierarchy cha;

  public ClassBasedInstanceKeys(AnalysisOptions options, IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("null cha");
    }
    this.cha = cha;
    this.options = options;
  }

  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    if (allocation == null) {
      throw new IllegalArgumentException("allocation is null");
    }

    if (options.getClassTargetSelector() == null) {
      throw new IllegalStateException("options did not specify class target selector");
    }
    IClass type = options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    if (type == null) {
      return null;
    }

    ConcreteTypeKey key = new ConcreteTypeKey(type);

    return key;
  }

  /**
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory#getInstanceKeyForMultiNewArray(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.NewSiteReference, int) dim == 0 represents the first dimension, e.g., the [Object; instances in
   *      [[Object; e.g., the [[Object; instances in [[[Object; dim == 1 represents the second dimension, e.g., the [Object
   *      instances in [[[Object;
   */
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    if (DEBUG) {
      System.err.println(("getInstanceKeyForMultiNewArray " + allocation + " " + dim));
    }
    ArrayClass type = (ArrayClass) options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    assert (type != null);
    if (DEBUG) {
      System.err.println(("type: " + type));
    }
    if (type == null) {
      assert type != null : "null type for " + allocation;
    }
    int i = 0;
    while (i <= dim) {
      i++;
      if (type == null) {
        Assertions.UNREACHABLE();
      }
      type = (ArrayClass) type.getElementClass();
      if (DEBUG) {
        System.err.println(("intermediate: " + i + " " + type));
      }
    }
    if (DEBUG) {
      System.err.println(("final type: " + type));
    }
    if (type == null) {
      return null;
    }
    ConcreteTypeKey key = new ConcreteTypeKey(type);

    return key;
  }

  public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
    if (type == null || cha.lookupClass(type) == null) {
      return null;
    } else {
      if (options.getUseConstantSpecificKeys()) {
        return new ConstantKey<T>(S, cha.lookupClass(type));
      } else {
        return new ConcreteTypeKey(cha.lookupClass(type));
      }
    }
  }

  /**
   * @return a set of ConcreteTypeKeys that represent the exceptions the PEI may throw.
   */
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter peiLoc, TypeReference type) {
    IClass klass = cha.lookupClass(type);
    if (klass == null) {
      return null;
    }
    return new ConcreteTypeKey(cha.lookupClass(type));
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    IClass klass = cha.lookupClass(type);
    if (klass == null) {
      return new ConcreteTypeKey(cha.lookupClass(TypeReference.JavaLangClass));
    } else {
      // return the IClass itself, wrapped as a constant!
      return new ConstantKey<IClass>(klass, cha.lookupClass(TypeReference.JavaLangClass));
    }

  }

  /**
   * @return Returns the class hierarchy.
   */
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

}
