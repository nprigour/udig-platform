package org.locationtech.udig.printing.model.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Overrides delete methods of ECoreUtil to handle bug with wrong casting to EObject
 * that leads to a ClassCastException.
 * 
 * @author Nikolaos Pringouris <nprigour@gmail.com>
 *
 */
public class EcoreUtilWrapper {

	
    public static class UsageCrossReferencerExt extends EcoreUtil.UsageCrossReferencer {

		protected UsageCrossReferencerExt(EObject arg0) {
			super(arg0);
		}


		protected UsageCrossReferencerExt(Collection<?> arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}


		protected UsageCrossReferencerExt(Resource arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}


		protected UsageCrossReferencerExt(ResourceSet arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		} 	
		
	    /**
	     * Modifies behavior of method to check for an EObject instance prior to casting.
	     *  
	     * @param eObject
	     *
		 * @see org.eclipse.emf.ecore.util.EcoreUtil.CrossReferencer#handleCrossReference(org.eclipse.emf.ecore.EObject)
		 */
    	@Override
    	protected void handleCrossReference(EObject eObject) {
    		InternalEObject internalEObject = (InternalEObject)eObject;
    		for (EContentsEList.FeatureIterator<EObject> crossReferences = getCrossReferences(internalEObject); crossReferences.hasNext();)
    		{
    			Object obj = crossReferences.next();
    			if (obj instanceof EObject) {
    				EObject crossReferencedEObject = (EObject) obj;
    				if (crossReferencedEObject != null)
    				{
    					EReference eReference = (EReference)crossReferences.feature();
    					if (crossReference(internalEObject, eReference, crossReferencedEObject))
    					{
    						add(internalEObject, eReference, crossReferencedEObject);
    					}
    				}
    			}
    		}
    	}

    	
		/* (non-Javadoc)
		 * @see org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer#findUsage(org.eclipse.emf.ecore.EObject)
		 */
		@Override
		protected Collection<Setting> findUsage(EObject eObject) {
			// TODO Auto-generated method stub
			return super.findUsage(eObject);
		}


		/* (non-Javadoc)
		 * @see org.eclipse.emf.ecore.util.EcoreUtil.UsageCrossReferencer#findAllUsage(java.util.Collection)
		 */
		@Override
		protected java.util.Map<EObject, Collection<Setting>> findAllUsage(
				Collection<?> eObjectsOfInterest) {
			// TODO Auto-generated method stub
			return super.findAllUsage(eObjectsOfInterest);
		}
		
    }
    

    /**
     * Wrapper method that performs similar to @link {@link EcoreUtil#delete(EObject)}
     * 
     * @param eObject
     */
    public static void delete(EObject eObject)
    {
      EObject rootEObject = EcoreUtil.getRootContainer(eObject);
      Resource resource = rootEObject.eResource();

      Collection<EStructuralFeature.Setting> usages;
      if (resource == null)
      {
        usages = new UsageCrossReferencerExt(eObject).findUsage(rootEObject);
      }
      else
      {
        ResourceSet resourceSet = resource.getResourceSet();
        if (resourceSet == null)
        {
          usages = new UsageCrossReferencerExt(resource).findUsage(eObject);
        }
        else
        {
          usages = new UsageCrossReferencerExt(resourceSet).findUsage(eObject);
        }
      }

      for (EStructuralFeature.Setting setting : usages)
      {
        if (setting.getEStructuralFeature().isChangeable())
        {
          EcoreUtil.remove(setting, eObject);
        }
      }

      EcoreUtil.remove(eObject);
    }
    
    
    /**
     * Wrapper method that performs similar to @link {@link EcoreUtil#delete(EObject, boolean)}
     * @param eObject
     * @param recursive
     */
    public static void delete(EObject eObject, boolean recursive)
    {
      if (recursive)
      {
        EObject rootEObject = EcoreUtil.getRootContainer(eObject);
        Resource resource = rootEObject.eResource();

        Set<EObject> eObjects = new HashSet<EObject>();        
        Set<EObject> crossResourceEObjects = new HashSet<EObject>();        
        eObjects.add(eObject);
        for (@SuppressWarnings("unchecked") TreeIterator<InternalEObject> j = (TreeIterator<InternalEObject>)(TreeIterator<?>)eObject.eAllContents();  j.hasNext(); )
        {
          InternalEObject childEObject = j.next();
          if (childEObject.eDirectResource() != null)
          {
            crossResourceEObjects.add(childEObject);
          }
          else
          {
            eObjects.add(childEObject);
          }
        }

        java.util.Map<EObject, Collection<EStructuralFeature.Setting>> usages;
        if (resource == null)
        {
          usages = new UsageCrossReferencerExt(rootEObject).findAllUsage(eObjects);
        }
        else
        {
          ResourceSet resourceSet = resource.getResourceSet();
          if (resourceSet == null)
          {
            usages = new UsageCrossReferencerExt(resource).findAllUsage(eObjects);
          }
          else
          {
            usages = new UsageCrossReferencerExt(resourceSet).findAllUsage(eObjects);
          }
        }

        for (java.util.Map.Entry<EObject, Collection<EStructuralFeature.Setting>> entry : usages.entrySet())
        {
          EObject deletedEObject = entry.getKey();
          Collection<EStructuralFeature.Setting> settings = entry.getValue();
          for (EStructuralFeature.Setting setting : settings)
          {
            if (!eObjects.contains(setting.getEObject()) && setting.getEStructuralFeature().isChangeable())
            {
              EcoreUtil.remove(setting, deletedEObject);
            }
          }
        }
    
        EcoreUtil.remove(eObject);

        for (EObject crossResourceEObject : crossResourceEObjects)
        {
        	EcoreUtil.remove(crossResourceEObject.eContainer(), crossResourceEObject.eContainmentFeature(), crossResourceEObject);
        }
      }
      else
      {
        delete(eObject);
      }
    }
}
