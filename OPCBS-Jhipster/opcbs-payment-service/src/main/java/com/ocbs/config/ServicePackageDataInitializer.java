package com.ocbs.config;

import com.ocbs.domain.ServicePackage;
import com.ocbs.repository.ServicePackageRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServicePackageDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePackageDataInitializer.class);

    @Bean
    public CommandLineRunner initServicePackages(ServicePackageRepository packageRepository) {
        return args -> {
            if (packageRepository.count() == 0) {
                LOG.info("🚀 Seeding default platform ServicePackages for Doctors...");

                ServicePackage pkg1 = new ServicePackage();
                pkg1.setName("Standard Doctor Tier");
                pkg1.setDescription("Gói cơ bản dành cho Bác sĩ mới tham gia nền tảng MindBridge, tối đa 30 bệnh nhân.");
                pkg1.setDurationDays(30);
                pkg1.setPrice(new BigDecimal("499000.00"));
                pkg1.setMaxPatientCapacity(30);
                pkg1.setMaxDailySlotsCapacity(8);
                pkg1.setIsActive(true);
                pkg1.setIsFeatured(false);
                pkg1.setDisplayOrder(1);

                ServicePackage pkg2 = new ServicePackage();
                pkg2.setName("Pro Specialist Tier");
                pkg2.setDescription("Gói chuyên nghiệp không giới hạn lịch hẹn, ưu tiên đề xuất trên trang chủ.");
                pkg2.setDurationDays(90);
                pkg2.setPrice(new BigDecimal("999000.00"));
                pkg2.setMaxPatientCapacity(100);
                pkg2.setMaxDailySlotsCapacity(20);
                pkg2.setIsActive(true);
                pkg2.setIsFeatured(true);
                pkg2.setDisplayOrder(2);

                ServicePackage pkg3 = new ServicePackage();
                pkg3.setName("VIP Enterprise Clinic");
                pkg3.setDescription("Gói toàn diện cho phòng khám & chuyên gia hàng đầu với bảo hiểm và hỗ trợ 24/7.");
                pkg3.setDurationDays(365);
                pkg3.setPrice(new BigDecimal("1999000.00"));
                pkg3.setMaxPatientCapacity(500);
                pkg3.setMaxDailySlotsCapacity(50);
                pkg3.setIsActive(true);
                pkg3.setIsFeatured(false);
                pkg3.setDisplayOrder(3);

                packageRepository.saveAll(List.of(pkg1, pkg2, pkg3));
                LOG.info("🎉 Seeded 3 ServicePackages successfully!");
            }
        };
    }
}
