package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class ScoringTripImpl extends NumberedScoreBoardEventProviderImpl<ScoringTrip> implements ScoringTrip {
    ScoringTripImpl(TeamJam parent, int number) {
        super(parent, number, TeamJam.SCORING_TRIP);
        addProperties(SCORE, AFTER_S_P, CURRENT, DURATION, JAM_CLOCK_START, JAM_CLOCK_END, ANNOTATION, INSERT_BEFORE,
                REMOVE);
        setCopy(JAM_CLOCK_START, this, PREVIOUS, JAM_CLOCK_END, true);
        setRecalculated(DURATION).addSource(this, JAM_CLOCK_END).addSource(this, JAM_CLOCK_START);
        set(AFTER_S_P, hasPrevious() ? getPrevious().get(AFTER_S_P) : false);
    }

    @Override
    public Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == SCORE && (Integer) value < 0) { return 0; }
        if (prop == DURATION) {
            if (get(JAM_CLOCK_END) > 0L) {
                return get(JAM_CLOCK_END) - get(JAM_CLOCK_START);
            } else {
                return 0L;
            }
        }
        return value;
    }
    @Override
    public void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if ((prop == SCORE || (prop == CURRENT && !(Boolean) value)) && get(JAM_CLOCK_END) == 0L) {
            set(JAM_CLOCK_END, scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
        }
        if (prop == CURRENT && (Boolean) value && get(SCORE) == 0) {
            set(JAM_CLOCK_END, 0L);
        }
        if (prop == AFTER_S_P) {
            if ((Boolean) value && hasNext()) {
                getNext().set(AFTER_S_P, true);
            }
            if (!(Boolean) value && hasPrevious()) {
                getPrevious().set(AFTER_S_P, false);
            }
            if (flag != Flag.SPECIAL_CASE) {
                if ((Boolean) value && (!hasPrevious() || !getPrevious().get(AFTER_S_P))) {
                    parent.set(TeamJam.STAR_PASS_TRIP, this);
                } else if (!(Boolean) value && (!hasNext() || getNext().get(AFTER_S_P))) {
                    parent.set(TeamJam.STAR_PASS_TRIP, getNext());
                }
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == REMOVE) {
            if (getParent().numberOf(TeamJam.SCORING_TRIP) > 1) {
                delete(source);
            } else {
                // We cannot remove the initial trip when it is the only trip, so set its score
                // to 0.
                set(SCORE, 0);
                set(JAM_CLOCK_END, 0L);
                set(ANNOTATION, "");
            }
        } else if (prop == INSERT_BEFORE) {
            parent.add(ownType, new ScoringTripImpl((TeamJam) parent, getNumber()));
        }
    }

    @Override
    public int getScore() { return get(SCORE); }
}
