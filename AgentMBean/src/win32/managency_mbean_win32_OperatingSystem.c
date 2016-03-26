#include <managency_mbean_win32_OperatingSystem.h>
#include <windows.h>

/* Uptime */
JNIEXPORT jlong JNICALL Java_managency_mbean_win32_OperatingSystem_getUptime(JNIEnv *env, jobject obj) {
	return GetTickCount();
}

/* Memory */

JNIEXPORT jlong JNICALL Java_managency_mbean_win32_OperatingSystem_getTotalPhysicalMemory(JNIEnv *env, jobject obj) {
	MEMORYSTATUS memoryStatus;
	GlobalMemoryStatus(&memoryStatus);
	return memoryStatus.dwTotalPhys;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_win32_OperatingSystem_getAvailablePhysicalMemory(JNIEnv *env, jobject obj) {
	MEMORYSTATUS memoryStatus;
	GlobalMemoryStatus(&memoryStatus);
	return memoryStatus.dwAvailPhys;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_win32_OperatingSystem_getTotalVirtualMemory(JNIEnv *env, jobject obj) {
	MEMORYSTATUS memoryStatus;
	GlobalMemoryStatus(&memoryStatus);
	return memoryStatus.dwTotalVirtual;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_win32_OperatingSystem_getAvailableVirtualMemory(JNIEnv *env, jobject obj) {
	MEMORYSTATUS memoryStatus;
	GlobalMemoryStatus(&memoryStatus);
	return memoryStatus.dwAvailVirtual;
}

/* Processor */

static LONG queryProcessorRegKey(LPCTSTR keyName, LPBYTE data, LPDWORD dataSize) {
	HKEY hkey;
	LONG rc = RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Hardware\\Description\\System\\CentralProcessor\\0", 0, KEY_QUERY_VALUE, &hkey);
	if(rc == ERROR_SUCCESS) {
		rc = RegQueryValueEx(hkey, keyName, NULL, NULL, data, dataSize);
		RegCloseKey(hkey);
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorNameBytes(JNIEnv *env, jobject obj, jbyteArray ansiBytes) {
	DWORD length = (*env)->GetArrayLength(env, ansiBytes);
	jbyte *elements = (*env)->GetByteArrayElements(env, ansiBytes, NULL);
	LONG rc = queryProcessorRegKey("ProcessorNameString", elements, &length);
	(*env)->ReleaseByteArrayElements(env, ansiBytes, elements, 0);
	return rc;
}
JNIEXPORT jint JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorVendorBytes(JNIEnv *env, jobject obj, jbyteArray ansiBytes) {
	DWORD length = (*env)->GetArrayLength(env, ansiBytes);
	jbyte *elements = (*env)->GetByteArrayElements(env, ansiBytes, NULL);
	LONG rc = queryProcessorRegKey("VendorIdentifier", elements, &length);
	(*env)->ReleaseByteArrayElements(env, ansiBytes, elements, 0);
	return rc;
}
JNIEXPORT jshort JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorArch(JNIEnv *env, jobject obj) {
	SYSTEM_INFO systemInfo;
	GetSystemInfo(&systemInfo);
	return systemInfo.wProcessorArchitecture;
}
JNIEXPORT jint JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorSpeed(JNIEnv *env, jobject obj) {
	DWORD data;
	DWORD dataSize = sizeof(data);
	LONG rc = queryProcessorRegKey("~MHz", (LPBYTE) &data, &dataSize);
	if(rc == ERROR_SUCCESS)
		return data;
	else
		return -1;
}
JNIEXPORT jshort JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorFamily(JNIEnv *env, jobject obj) {
	SYSTEM_INFO systemInfo;
	GetSystemInfo(&systemInfo);
	return systemInfo.wProcessorLevel;
}
JNIEXPORT jshort JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorModel(JNIEnv *env, jobject obj) {
	SYSTEM_INFO systemInfo;
	GetSystemInfo(&systemInfo);
	return (systemInfo.wProcessorRevision>>1 & 0x00FF);
}
JNIEXPORT jshort JNICALL Java_managency_mbean_win32_OperatingSystem_getProcessorStepping(JNIEnv *env, jobject obj) {
	SYSTEM_INFO systemInfo;
	GetSystemInfo(&systemInfo);
	return (systemInfo.wProcessorRevision & 0x00FF);
}

JNIEXPORT jboolean JNICALL Java_managency_mbean_win32_OperatingSystem_isMMXProcessor(JNIEnv *env, jobject obj) {
	return IsProcessorFeaturePresent(PF_MMX_INSTRUCTIONS_AVAILABLE);
}
JNIEXPORT jboolean JNICALL Java_managency_mbean_win32_OperatingSystem_isSSEProcessor(JNIEnv *env, jobject obj) {
	return IsProcessorFeaturePresent(PF_XMMI_INSTRUCTIONS_AVAILABLE);
}
JNIEXPORT jboolean JNICALL Java_managency_mbean_win32_OperatingSystem_isSSE2Processor(JNIEnv *env, jobject obj) {
	return IsProcessorFeaturePresent(PF_XMMI64_INSTRUCTIONS_AVAILABLE);
}
JNIEXPORT jboolean JNICALL Java_managency_mbean_win32_OperatingSystem_is3DNowProcessor(JNIEnv *env, jobject obj) {
	return IsProcessorFeaturePresent(PF_3DNOW_INSTRUCTIONS_AVAILABLE);
}
