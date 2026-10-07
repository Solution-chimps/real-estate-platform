package com.br.real_estate_platform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PropertyStatusTest {

	@Test
	void draftsCanBePublishedOrArchived() {
		assertThat(PropertyStatus.DRAFT.canTransitionTo(PropertyStatus.PUBLISHED)).isTrue();
		assertThat(PropertyStatus.DRAFT.canTransitionTo(PropertyStatus.ARCHIVED)).isTrue();
	}

	@Test
	void publishedListingsCanBeUnpublishedOrArchived() {
		assertThat(PropertyStatus.PUBLISHED.canTransitionTo(PropertyStatus.DRAFT)).isTrue();
		assertThat(PropertyStatus.PUBLISHED.canTransitionTo(PropertyStatus.ARCHIVED)).isTrue();
	}

	@Test
	void archivedListingsMustBeRestoredAsDraftBeforePublishing() {
		assertThat(PropertyStatus.ARCHIVED.canTransitionTo(PropertyStatus.DRAFT)).isTrue();
		assertThat(PropertyStatus.ARCHIVED.canTransitionTo(PropertyStatus.PUBLISHED)).isFalse();
	}

	@Test
	void keepingTheSameStatusIsAlwaysAllowed() {
		for (PropertyStatus status : PropertyStatus.values()) {
			assertThat(status.canTransitionTo(status)).isTrue();
		}
	}
}
