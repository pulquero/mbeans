#include <managency_mbean_linux_OperatingSystem.h>
#include <sys/sysinfo.h>

/* Uptime */
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getUptime(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return ((jlong)1000)*((jlong)si.uptime);
}

/* Memory */

JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getTotalPhysicalMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.totalram;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getAvailablePhysicalMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.freeram;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getTotalVirtualMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.totalswap;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getAvailableVirtualMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.freeswap;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getSharedMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.sharedram;
}
JNIEXPORT jlong JNICALL Java_managency_mbean_linux_OperatingSystem_getBufferMemory(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.bufferram;
}

JNIEXPORT jshort JNICALL Java_managency_mbean_linux_OperatingSystem_getProcessCount(JNIEnv *env, jobject obj) {
	struct sysinfo si;
	sysinfo(&si);
	return si.procs;
}
