#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include "org_hwbot_cpuid_CpuId.h"

struct cpuid {
	uint32_t eax;
	uint32_t ebx;
	uint32_t ecx;
	uint32_t edx;
} __attribute__((packed));
typedef struct cpuid cpuid_t;

static inline void
cpuid(struct cpuid *r, uint32_t eax)
{

	__asm__ __volatile__("cpuid"
				: "=a" (r->eax),
				  "=b" (r->ebx),
				  "=c" (r->ecx),
				  "=d" (r->edx)
				: "a"  (eax)
				: "memory");

	return;
}

void
cpuid_brand(char *buffer, size_t length)
{
	char *p = buffer;
	struct cpuid r;
	uint32_t i;

	cpuid(&r, 0x80000000);
	if ((r.eax & 0x80000000) == 0 ||
	     r.eax < 0x80000004       ||
	     length <= 1) 
		goto unsupported;

	length -= 1;
	for (i = 0x80000002; i <= 0x80000004; i++) {
		cpuid(&r, i);
		if (length <= 16) {
			memcpy(p, &r, length);
			p += length;
			break;
		}

		memcpy(p, &r, 16);
		length -= 16;
		p += 16;
	}

	*p = '\0';
	return;

unsupported:
	*buffer = '\0';
	return;
}


 JNIEXPORT jstring JNICALL Java_org_hwbot_cpuid_CpuId_model(JNIEnv *env, jobject obj)
 {
 	char buffer[200];
 	cpuid_brand(buffer, sizeof(buffer));
 	puts("hello");
   	return (*env)->NewStringUTF(env, buffer);
 }


#ifdef __i386
JNIEXPORT jlong JNICALL Java_org_hwbot_cpuid_CpuId_rdtsc(JNIEnv *env, jobject obj) {
  uint64_t x;
  __asm__ volatile ("rdtsc" : "=A" (x));
  return x;
}
#elif defined __amd64
JNIEXPORT jlong JNICALL Java_org_hwbot_cpuid_CpuId_rdtsc(JNIEnv *env, jobject obj) {
  uint64_t a, d;
  __asm__ volatile ("rdtsc" : "=a" (a), "=d" (d));
  return (d<<32) | a;
}
#endif



