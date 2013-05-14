/*
 * Copyright 2009, 2010 Samy Al Bahra.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include "cpuid.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define FEATURES (1 << 0)
#define VENDOR   (1 << 1)
#define BRAND    (1 << 2)
#define ADDRESS  (1 << 3)

static void
usage(FILE *fp, int status)
{

	fprintf(fp, "Usage: cpuid -[a|b|f|h|v|s]\n");
	exit(status);
}

int
main(int argc, char *argv[])
{
	unsigned int i;
	const char *e_columns, *s;
	unsigned int columns, offset;
	uint8_t physical, linear;
	char buffer[CPUID_BRAND_LENGTH];

	if (argc != 2) 
		usage(stderr, EXIT_FAILURE);

	if (*argv[1] != '-' || strlen(argv[1]) != 2) 
		usage(stderr, EXIT_FAILURE);

	switch (argv[1][1]) {
	case 'a':
		cpuid_address_size(&physical, &linear);
		printf("P:%u V:%u\n", physical, linear);
		break;
	case 'b':
		cpuid_brand(buffer, sizeof(buffer));
		puts(buffer);
		break;
	case 'f':
		e_columns = getenv("COLUMNS");
		if (e_columns == NULL || *e_columns == '\0') 
			columns = 80;
		else 
			columns = atoi(e_columns);

		offset = 0;

		for (i = 0; i < CPUID_FEATURE_LENGTH; i++) {
			if (cpuid_feature(i)) {
				s = cpuid_feature_string(i);
				if (offset + strlen(s) + 1 > columns) {
					putchar('\n');
					offset = 0;
				}
				offset += printf("%s ", cpuid_feature_string(i));
			}
		}
		putchar('\n');
		break;
	case 'v':
		printf("%s\n", cpuid_vendor_string(cpuid_vendor()));
		break;
	case 's':
		;
		uint64_t startCount = rdtsc();
		usleep(500000);
		uint64_t endCount = rdtsc();
		printf("%llu", (endCount - startCount) * 2 / 1000000);
		break;
	case 'h':
		usage(stdout, EXIT_SUCCESS);
		break;
	default:
		usage(stderr, EXIT_FAILURE);
	}

	return 0;
}
