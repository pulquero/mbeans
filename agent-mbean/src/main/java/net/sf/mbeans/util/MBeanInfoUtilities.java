package net.sf.mbeans.util;

import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

public final class MBeanInfoUtilities {
	private MBeanInfoUtilities() {}

	/**
	 * Returns a constructor that takes the specified number of parameters.
	 */
	public static MBeanConstructorInfo getConstructor(MBeanInfo info, int paramCount) {
		MBeanConstructorInfo[] cnstrs = info.getConstructors();
                for(int i=0; i<cnstrs.length; i++) {
                        if(cnstrs[i].getSignature().length == paramCount)
				return cnstrs[i];
                }
                return null;
        }

	/**
	 * Returns the attribute with the specified name.
	 */
	public static MBeanAttributeInfo getAttribute(MBeanInfo info, String attrName) {
		MBeanAttributeInfo[] attrs = info.getAttributes();
                for(int i=0; i<attrs.length; i++) {
                        if(attrs[i].getName().equals(attrName))
                                return attrs[i];
                }
                return null;
        }
	public static boolean hasAttribute(MBeanInfo info, String attrName) {
		return getAttribute(info, attrName) != null;
	}

	/**
	 * Returns the first operation with the specified name.
	 */
	public static MBeanOperationInfo getOperation(MBeanInfo info, String opName) {
		MBeanOperationInfo[] ops = info.getOperations();
                for(int i=0; i<ops.length; i++) {
                        if(ops[i].getName().equals(opName))
                                return ops[i];
                }
                return null;
        }
	public static boolean hasOperation(MBeanInfo info, String opName) {
		return getOperation(info, opName) != null;
	}

	/**
	 * Returns the first operation that takes the specified number of parameters.
	 */
	public static MBeanOperationInfo getOperation(MBeanInfo info, String opName, int paramCount) {
		MBeanOperationInfo[] ops = info.getOperations();
                for(int i=0; i<ops.length; i++) {
                        if(ops[i].getName().equals(opName) && ops[i].getSignature().length == paramCount)
				return ops[i];
                }
                return null;
        }
	public static boolean hasOperation(MBeanInfo info, String opName, int paramCount) {
		return getOperation(info, opName, paramCount) != null;
	}

	/**
	 * Returns the operation with the specified name and signature.
	 * @param signature can be null.
	 */
	public static MBeanOperationInfo getOperation(MBeanInfo info, String opName, String[] signature) {
		if(signature == null)
			signature = new String[0];

		MBeanOperationInfo[] ops = info.getOperations();
		for(int i=0; i<ops.length; i++) {
			MBeanOperationInfo op = ops[i];
			if(op.getName().equals(opName)) {
				MBeanParameterInfo[] params = op.getSignature();
				if(params.length == signature.length) {
					boolean found = true;
					for(int j=0; found && j<params.length; j++) {
						if(!params[j].getType().equals(signature[j]))
							found = false;
					}
					if(found)
						return op;
				}
			}
		}
		return null;
	}
	public static boolean hasOperation(MBeanInfo info, String opName, String[] signature) {
		return getOperation(info, opName, signature) != null;
	}

	/**
	 * Returns the operation with the specified name, signature and return type.
	 * @param signature can be null.
	 */
	public static MBeanOperationInfo getOperation(MBeanInfo info, String opName, String[] signature, String returnType) {
		if(signature == null)
			signature = new String[0];

		MBeanOperationInfo[] ops = info.getOperations();
		for(int i=0; i<ops.length; i++) {
			MBeanOperationInfo op = ops[i];
			if(op.getName().equals(opName) && op.getReturnType().equals(returnType)) {
				MBeanParameterInfo[] params = op.getSignature();
				if(params.length == signature.length) {
					boolean found = true;
					for(int j=0; found && j<params.length; j++) {
						if(!params[j].getType().equals(signature[j]))
							found = false;
					}
					if(found)
						return op;
				}
			}
		}
		return null;
	}
	public static boolean hasOperation(MBeanInfo info, String opName, String[] signature, String returnType) {
		return getOperation(info, opName, signature, returnType) != null;
	}

	public static boolean hasStartOperation(MBeanInfo info) {
		return hasOperation(info, MBeanUtilities.START_OPERATION_NAME, null, Void.TYPE.getName());
	}
	public static boolean hasStopOperation(MBeanInfo info) {
		return hasOperation(info, MBeanUtilities.STOP_OPERATION_NAME, null, Void.TYPE.getName());
	}
}
