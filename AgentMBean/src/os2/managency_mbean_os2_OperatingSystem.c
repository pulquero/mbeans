#include <managency_mbean_os2_OperatingSystem.h>
#define INCL_DOS
#include <os2.h>

JNIEXPORT jlong JNICALL Java_managency_mbean_os2_OperatingSystem_getSysInfo(JNIEnv *env, jclass cls, jshort index) {
	ULONG value;
	APIRET rc = DosQuerySysInfo(index, index, &value, sizeof(value));
	jlong jvalue;
	if(rc > 0) {
		// jvalue = int2ll(-rc);
		jvalue.hi = 0xFFFFFFFF;
		jvalue.lo = 0xFFFFFFFF;
	} else {
		// jvalue = uint2ll(value);
		jvalue.hi = 0;
		jvalue.lo = value;
	}
	return jvalue;
}
