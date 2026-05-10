package com.microservices.branch.config;

import com.microservices.branch.entity.Branch;
import com.microservices.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds two fixed branches when the database is empty so registration can reference stable IDs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BranchSampleDataLoader implements ApplicationRunner {

    public static final String SEED_BRANCH_1_ID = "a0000001-0000-4000-8000-000000000001";
    public static final String SEED_BRANCH_2_ID = "a0000002-0000-4000-8000-000000000002";

    private final BranchRepository branchRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (branchRepository.count() > 0) {
            return;
        }

        Branch downtown = Branch.builder()
                .id(SEED_BRANCH_1_ID)
                .name("Downtown Kitchen")
                .address("123 Main Street, City Center")
                .phone("+966501234567")
                .description("Flagship branch — dine-in and takeaway.")
                .active(true)
                .latitude(24.7136)
                .longitude(46.6753)
                .rating(4.5)
                .build();

        Branch uptown = Branch.builder()
                .id(SEED_BRANCH_2_ID)
                .name("Uptown Express")
                .address("456 North Avenue")
                .phone("+966507654321")
                .description("Quick service branch.")
                .active(true)
                .latitude(24.7500)
                .longitude(46.7000)
                .rating(4.2)
                .build();

        branchRepository.save(downtown);
        branchRepository.save(uptown);
        log.info("Seeded {} sample branches (empty DB)", 2);
    }
}
