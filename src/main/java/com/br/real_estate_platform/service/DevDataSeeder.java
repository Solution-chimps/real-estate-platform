package com.br.real_estate_platform.service;

import com.br.real_estate_platform.entity.Contact;
import com.br.real_estate_platform.entity.Property;
import com.br.real_estate_platform.entity.PropertyCodeCounter;
import com.br.real_estate_platform.entity.PropertyPurpose;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.entity.PropertyType;
import com.br.real_estate_platform.repository.ContactRepository;
import com.br.real_estate_platform.repository.PropertyCodeCounterRepository;
import com.br.real_estate_platform.repository.PropertyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

	private static final int NEXT_CODE_AFTER_SEED = 143;
	private static final String CITY = "São Caetano do Sul";

	private final PropertyRepository propertyRepository;
	private final ContactRepository contactRepository;
	private final PropertyCodeCounterRepository counterRepository;
	private final Clock clock;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (propertyRepository.count() > 0) {
			return;
		}
		Instant now = clock.instant();
		List<Property> properties = List.of(
				property(142, "Casa contemporânea com jardim",
						"Casa térrea com pé-direito alto, integração total entre sala, cozinha e jardim privativo. Três suítes amplas, escritório com entrada independente e churrasqueira coberta. Rua arborizada e tranquila, a poucos minutos do Parque Chico Mendes.",
						PropertyPurpose.SALE, PropertyType.HOUSE, "Santa Paula", "1850000", null, "6400", "280", 3, 3, 4, 2,
						List.of("Jardim privativo", "Churrasqueira", "Cozinha planejada", "Ar-condicionado", "Escritório", "Rua arborizada"),
						null, true, PropertyStatus.PUBLISHED, now),
				property(139, "Apartamento com vista e varanda",
						"Apartamento de dois dormitórios em andar alto, com varanda gourmet voltada para o nascente e vista aberta. Condomínio com piscina, academia e salão de festas. Prédio com portaria 24 horas e vaga coberta.",
						PropertyPurpose.SALE, PropertyType.APARTMENT, "Barcelona", "790000", "980", "2100", "84", 2, 1, 2, 1,
						List.of("Varanda gourmet", "Piscina", "Academia", "Portaria 24h", "Andar alto"),
						null, true, PropertyStatus.PUBLISHED, now),
				property(137, "Studio mobiliado",
						"Studio completo para estadias curtas: cama queen, cozinha equipada, internet de alta velocidade e roupa de cama inclusa. A duas quadras da Avenida Goiás e perto do metrô de superfície.",
						PropertyPurpose.SHORT_STAY, PropertyType.APARTMENT, "Santa Paula", "320", null, null, "32", 1, 0, 1, 0,
						List.of("Mobiliado", "Internet", "Roupa de cama", "Cozinha equipada"),
						null, true, PropertyStatus.PUBLISHED, now),
				property(136, "Apartamento familiar",
						"Três dormitórios com armários planejados, sala para dois ambientes e cozinha com despensa. Duas vagas demarcadas e depósito individual. Condomínio com playground e quadra, ao lado de escolas e do comércio da Rua Manoel Coelho.",
						PropertyPurpose.RENT, PropertyType.APARTMENT, "Barcelona", "4800", "1150", "2600", "110", 3, 1, 2, 2,
						List.of("Armários planejados", "Playground", "Quadra", "Depósito", "Duas vagas"),
						LocalDate.of(2026, 11, 1), true, PropertyStatus.PUBLISHED, now),
				property(135, "Sala comercial pronta",
						"Sala comercial com piso elevado, ar-condicionado e copa, pronta para ocupação. Edifício com recepção, elevadores novos e estacionamento rotativo para clientes.",
						PropertyPurpose.RENT, PropertyType.COMMERCIAL, "Centro", "3200", "720", "1800", "45", 0, 0, 1, 1,
						List.of("Ar-condicionado", "Copa", "Recepção", "Piso elevado"),
						null, false, PropertyStatus.DRAFT, now),
				property(131, "Sobrado reformado",
						"Sobrado reformado em 2025 com três dormitórios, sala ampla com lareira e quintal com área gourmet. Elétrica e hidráulica novas, aquecimento solar e duas vagas cobertas.",
						PropertyPurpose.SALE, PropertyType.HOUSE, "Olímpico", "1250000", null, "4300", "190", 3, 1, 3, 2,
						List.of("Reformado", "Lareira", "Área gourmet", "Aquecimento solar"),
						null, false, PropertyStatus.DRAFT, now));
		propertyRepository.saveAll(properties);

		Contact visit = new Contact();
		visit.setId(UUID.randomUUID());
		visit.setProperty(properties.get(0));
		visit.setName("Mariana Alves");
		visit.setPhone("(11) 98877-1234");
		visit.setEmail("mariana.alves@example.com");
		visit.setMessage("Gostaria de agendar uma visita no sábado pela manhã.");
		visit.setRead(false);
		visit.setCreatedAt(now);

		Contact appraisal = new Contact();
		appraisal.setId(UUID.randomUUID());
		appraisal.setName("Carlos Menezes");
		appraisal.setPhone("(11) 97654-9988");
		appraisal.setEmail("carlos.menezes@example.com");
		appraisal.setMessage("Tenho um apartamento no bairro Cerâmica e quero avaliar para venda.");
		appraisal.setRead(true);
		appraisal.setCreatedAt(now.minusSeconds(172_800));
		contactRepository.saveAll(List.of(visit, appraisal));

		counterRepository.findById(PropertyCodeCounter.SINGLETON_ID)
				.ifPresent(counter -> counter.setNextValue(NEXT_CODE_AFTER_SEED));
		log.info("Seeded {} properties and {} contacts for the dev profile", properties.size(), 2);
	}

	private static Property property(int codeNumber, String title, String description, PropertyPurpose purpose,
			PropertyType type, String neighborhood, String price, String condoFee, String propertyTax, String area,
			int bedrooms, int suites, int bathrooms, int parkingSpaces, List<String> features, LocalDate availableFrom,
			boolean featured, PropertyStatus status, Instant now) {
		Property property = new Property();
		property.setId(UUID.randomUUID());
		property.setCode(PropertyCodeGenerator.format(codeNumber));
		property.setTitle(title);
		property.setDescription(description);
		property.setPurpose(purpose);
		property.setType(type);
		property.setNeighborhood(neighborhood);
		property.setCity(CITY);
		property.setPrice(new BigDecimal(price));
		property.setCondoFee(condoFee == null ? null : new BigDecimal(condoFee));
		property.setPropertyTax(propertyTax == null ? null : new BigDecimal(propertyTax));
		property.setArea(new BigDecimal(area));
		property.setBedrooms(bedrooms);
		property.setSuites(suites);
		property.setBathrooms(bathrooms);
		property.setParkingSpaces(parkingSpaces);
		property.replaceFeatures(features);
		property.setAvailableFrom(availableFrom);
		property.setFeatured(featured);
		property.setStatus(status);
		property.setCreatedAt(now);
		property.setUpdatedAt(now);
		return property;
	}
}
