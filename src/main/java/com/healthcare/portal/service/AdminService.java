package com.healthcare.portal.service;

import com.healthcare.portal.dto.*;
import com.healthcare.portal.entity.*;
import com.healthcare.portal.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for Admin operations
 */
@Service
public class AdminService {
    
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final JobRepository jobRepository;
    private final InquiryRepository inquiryRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final BuyerVerificationRepository buyerVerificationRepository;
    private final ListingService listingService;
    private final JobService jobService;
    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public AdminService(UserRepository userRepository, ListingRepository listingRepository, JobRepository jobRepository, InquiryRepository inquiryRepository, JobApplicationRepository jobApplicationRepository, BuyerVerificationRepository buyerVerificationRepository, ListingService listingService, JobService jobService, UserService userService, EmailService emailService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.jobRepository = jobRepository;
        this.inquiryRepository = inquiryRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.buyerVerificationRepository = buyerVerificationRepository;
        this.listingService = listingService;
        this.jobService = jobService;
        this.userService = userService;
        this.emailService = emailService;
    }
    
    public AdminDTO.DashboardStats getDashboardStats() {
        AdminDTO.DashboardStats stats = new AdminDTO.DashboardStats();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);
        
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByStatus(User.UserStatus.ACTIVE));
        stats.setNewUsersToday(userRepository.countCreatedAfter(todayStart));
        stats.setNewUsersWeek(userRepository.countCreatedAfter(weekStart));
        
        stats.setTotalListings(listingRepository.count());
        stats.setPendingListings(listingRepository.countByStatus(Listing.ListingStatus.PENDING));
        stats.setActiveListings(listingRepository.countByStatus(Listing.ListingStatus.ACTIVE));
        stats.setNewListingsToday(listingRepository.countCreatedAfter(todayStart));
        
        stats.setTotalJobs(jobRepository.count());
        stats.setPendingJobs(jobRepository.countByStatus(Job.JobStatus.PENDING));
        stats.setActiveJobs(jobRepository.countByStatus(Job.JobStatus.ACTIVE));
        stats.setNewJobsWeek(jobRepository.countCreatedAfter(weekStart));
        
        stats.setInquiriesToday(inquiryRepository.countCreatedAfter(todayStart));
        stats.setPendingVerifications(buyerVerificationRepository.countByStatus(BuyerVerification.VerificationStatus.PENDING));
        stats.setApplicationsToday(jobApplicationRepository.countAppliedAfter(todayStart));
        
        return stats;
    }
    
    public Map<String, Object> getListingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Listings by status
        Map<String, Long> byStatus = new HashMap<>();
        for (Listing.ListingStatus status : Listing.ListingStatus.values()) {
            byStatus.put(status.name(), listingRepository.countByStatus(status));
        }
        stats.put("byStatus", byStatus);
        
        // Listings by category
        // This would require a custom query
        stats.put("total", listingRepository.count());
        
        return stats;
    }
    
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Users by status
        Map<String, Long> byStatus = new HashMap<>();
        for (User.UserStatus status : User.UserStatus.values()) {
            byStatus.put(status.name(), userRepository.countByStatus(status));
        }
        stats.put("byStatus", byStatus);
        stats.put("total", userRepository.count());
        stats.put("verifiedBuyers", userRepository.findByIsVerifiedBuyerTrue().size());
        
        return stats;
    }
    
    // Listing Management
    public PageResponse<ListingDTO.ListingSummaryResponse> getAllListings(
            int page, int size, String status, Long categoryId, Long dealTypeId, 
            Long cityId, Long stateId, Long sellerId, String keyword) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Listing.ListingStatus listingStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                listingStatus = Listing.ListingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }
        
        Page<Listing> listings = listingRepository.adminSearchListings(
                listingStatus, categoryId, dealTypeId, cityId, stateId, sellerId, keyword, pageable);
        
        Page<ListingDTO.ListingSummaryResponse> responsePage = listings.map(this::mapToListingSummary);
        
        return PageResponse.from(responsePage);
    }
    
    private ListingDTO.ListingSummaryResponse mapToListingSummary(Listing listing) {
        ListingDTO.ListingSummaryResponse response = new ListingDTO.ListingSummaryResponse();
        response.setId(listing.getId());
        response.setDisplayTitle(listing.getDisplayTitle());
        response.setShortDescription(listing.getShortDescription());
        response.setAskingPrice(listing.getAskingPrice());
        response.setPriceNegotiable(listing.getPriceNegotiable());
        response.setIsConfidential(listing.getIsConfidential());
        response.setStatus(listing.getStatus().name());
        response.setCategoryName(listing.getCategory() != null ? listing.getCategory().getName() : null);
        response.setDealType(listing.getDealType() != null ? listing.getDealType().getName() : null);
        if (listing.getCity() != null) {
            response.setCityName(listing.getCity().getName());
            if (listing.getCity().getState() != null) {
                response.setStateName(listing.getCity().getState().getName());
            }
        } else {
            response.setCityName(listing.getCityName());
        }
        response.setCreatedAt(listing.getCreatedAt());
        return response;
    }
    public ListingDTO.ListingResponse getListingById(Long id) {
        return listingService.getListingById(id);
    }
    
    @Transactional
    public void updateListing(Long id, ListingDTO.ListingRequest request) {
        listingService.updateListing(id, request);
    }
    
    @Transactional
    public void approveListing(Long id) {
        listingService.approveListing(id);
    }
    
    @Transactional
    public void rejectListing(Long id, String reason) {
        listingService.rejectListing(id, reason);
    }
    
    @Transactional
    public void deleteListing(Long id) {
        listingRepository.deleteById(id);
    }
    
    @Transactional
    public void featureListing(Long id, int days) {
        listingService.featureListing(id, days);
    }
    
    // Job Management
    public PageResponse<JobDTO.JobSummaryResponse> getAllJobs(
            int page, int size, String status, Long categoryId, Long cityId, 
            String employmentType, String experienceLevel, Long employerId, String keyword) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Job.JobStatus jobStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                jobStatus = Job.JobStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        
        Job.EmploymentType empType = null;
        if (employmentType != null && !employmentType.isEmpty()) {
            try {
                empType = Job.EmploymentType.valueOf(employmentType.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        
        Job.ExperienceLevel expLevel = null;
        if (experienceLevel != null && !experienceLevel.isEmpty()) {
            try {
                expLevel = Job.ExperienceLevel.valueOf(experienceLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        
        Page<Job> jobs = jobRepository.adminSearchJobs(
                jobStatus, categoryId, cityId, empType, expLevel, employerId, keyword, pageable);
        
        Page<JobDTO.JobSummaryResponse> responsePage = jobs.map(this::mapToJobSummary);
        
        return PageResponse.from(responsePage);
    }
    
    private JobDTO.JobSummaryResponse mapToJobSummary(Job job) {
        JobDTO.JobSummaryResponse response = new JobDTO.JobSummaryResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setSpecialisation(job.getSpecialisation());
        response.setEmploymentType(job.getEmploymentType().name());
        response.setSalaryMinLpa(job.getSalaryMinLpa());
        response.setSalaryMaxLpa(job.getSalaryMaxLpa());
        response.setExperienceRequired(job.getExperienceRequired().name());
        response.setCategoryName(job.getJobCategory() != null ? job.getJobCategory().getName() : null);
        if (job.getCity() != null) {
            response.setCityName(job.getCity().getName());
            if (job.getCity().getState() != null) {
                response.setStateName(job.getCity().getState().getName());
            }
        }
        response.setApplicationDeadline(job.getApplicationDeadline());
        if (job.getEmployer() != null) {
            response.setEmployerName(job.getEmployer().getFullName());
            response.setEmployerCompany(job.getEmployer().getCompanyName());
        }
        response.setCreatedAt(job.getCreatedAt());
        return response;
    }
    @Transactional
    public void approveJob(Long id) {
        jobService.approveJob(id);
    }
    
    @Transactional
    public void rejectJob(Long id, String reason) {
        jobService.rejectJob(id, reason);
    }
    
    @Transactional
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }
    
    // User Management
    public PageResponse<UserDTO.UserResponse> getAllUsers(int page, int size, String keyword, String role, String status, Boolean isVerified) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        User.UserStatus userStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                userStatus = User.UserStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                // Ignore invalid status
            }
        }

        UserRole.Role userRole = null;
        if (role != null && !role.isEmpty() && !role.equalsIgnoreCase("ALL")) {
            try {
                userRole = UserRole.Role.valueOf(role.toUpperCase());
            } catch (Exception e) {
                // Ignore invalid role
            }
        }
        
        Page<User> users = userRepository.searchUsers(keyword, userStatus, userRole, isVerified, pageable);
        Page<UserDTO.UserResponse> responsePage = users.map(userService::mapToUserResponse);
        
        return PageResponse.from(responsePage);
    }
    @Transactional
    public void suspendUser(Long userId, String reason) {
        userService.suspendUser(userId, reason);
    }
    
    @Transactional
    public void activateUser(Long userId) {
        userService.activateUser(userId);
    }
    
    @Transactional(readOnly = true)
    public AdminDTO.UserDetailResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        AdminDTO.UserDetailResponse response = new AdminDTO.UserDetailResponse();
        response.setProfile(userService.mapToUserResponse(user));
        
        // Activity counts
        response.setListingCount(listingRepository.countByUserId(userId));
        response.setJobCount(jobRepository.countByEmployerId(userId));
        response.setInquiryCount(inquiryRepository.countBySellerId(userId)); // Inquiries received
        response.setApplicationCount(jobApplicationRepository.countBySeekerId(userId)); // Applications sent
        
        return response;
    }

    @Transactional
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }
    
    // Verification Management
    public PageResponse<AdminDTO.AdminVerificationResponse> getPendingVerifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BuyerVerification> verifications = buyerVerificationRepository
                .findByStatus(BuyerVerification.VerificationStatus.PENDING, pageable);
        
        Page<AdminDTO.AdminVerificationResponse> responsePage = verifications.map(this::mapToAdminVerificationResponse);
        return PageResponse.from(responsePage);
    }
    
    private AdminDTO.AdminVerificationResponse mapToAdminVerificationResponse(BuyerVerification verification) {
        AdminDTO.AdminVerificationResponse response = new AdminDTO.AdminVerificationResponse();
        response.setId(verification.getId());
        response.setDocType(verification.getDocType() != null ? verification.getDocType().name() : null);
        response.setDocUrl(verification.getDocUrl());
        response.setStatus(verification.getStatus() != null ? verification.getStatus().name() : null);
        response.setRejectionReason(verification.getRejectionReason());
        response.setCreatedAt(verification.getCreatedAt());
        
        if (verification.getUser() != null) {
            AdminDTO.AdminVerificationResponse.UserSummary userSummary = new AdminDTO.AdminVerificationResponse.UserSummary();
            userSummary.setId(verification.getUser().getId());
            userSummary.setFullName(verification.getUser().getFullName());
            userSummary.setEmail(verification.getUser().getEmail());
            userSummary.setMobileNumber(verification.getUser().getMobileNumber());
            response.setUser(userSummary);
        }
        
        return response;
    }
    
    @Transactional
    public void approveVerification(Long id, Long adminId) {
        BuyerVerification verification = buyerVerificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));
        
        verification.setStatus(BuyerVerification.VerificationStatus.APPROVED);
        verification.setReviewedAt(LocalDateTime.now());
        
        User admin = userRepository.findById(adminId).orElse(null);
        verification.setReviewedBy(admin);
        
        buyerVerificationRepository.save(verification);
        
        // Update user verified status
        User user = verification.getUser();
        user.setIsVerifiedBuyer(true);
        userRepository.save(user);
        
        // Send buyer approved email
        emailService.sendBuyerApprovedEmail(user.getEmail(), user.getFullName());
    }
    
    @Transactional
    public void rejectVerification(Long id, Long adminId, String reason) {
        BuyerVerification verification = buyerVerificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));
        
        verification.setStatus(BuyerVerification.VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        verification.setReviewedAt(LocalDateTime.now());
        
        User admin = userRepository.findById(adminId).orElse(null);
        verification.setReviewedBy(admin);
        
        buyerVerificationRepository.save(verification);
    }
}
