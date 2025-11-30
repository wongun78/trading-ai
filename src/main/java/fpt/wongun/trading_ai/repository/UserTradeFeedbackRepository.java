package fpt.wongun.trading_ai.repository;

import fpt.wongun.trading_ai.domain.entity.UserTradeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTradeFeedbackRepository extends JpaRepository<UserTradeFeedback, Long> {
}
