package cybersoft.javabackend.java18.gamedoanso.service;

import cybersoft.javabackend.java18.gamedoanso.model.GameSession;
import cybersoft.javabackend.java18.gamedoanso.model.Guess;
import cybersoft.javabackend.java18.gamedoanso.model.Player;
import cybersoft.javabackend.java18.gamedoanso.repository.GameSessionRepository;
import cybersoft.javabackend.java18.gamedoanso.repository.GuessRepository;
import cybersoft.javabackend.java18.gamedoanso.repository.PlayerRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GameService {
    private static GameService INSTANCE = null;
    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;
    private final GuessRepository guessRepository;

    private GameService() {
        gameSessionRepository = new GameSessionRepository();
        playerRepository = new PlayerRepository();
        guessRepository = new GuessRepository();
    }

    public static GameService getINSTANCE() {
        if (INSTANCE == null)
            INSTANCE = new GameService();
        return INSTANCE;
    }

    public GameSession createGame(String username) {
        var gameSession = new GameSession(username);
        gameSession.setActive(true);

        // deactivate other games
        gameSessionRepository.deactivateAllGames(username);

        //cap nhat len db
        gameSessionRepository.save(gameSession);

        return gameSession;
    }

    public Player dangNhap(String username, String password) {
        Player player = playerRepository.findByUsername(username);

        if (player == null)
            return null;

        if (player.getPassword().equals(password))
            return player;

        return null;
    }

    public Player dangKy(String username, String password, String name) {
        if (!isValidUser(username, password, name))
            return null;

        boolean userExisted = playerRepository.existedByUsername(username);

        if (userExisted)
            return null;

        Player newUser = new Player(username, password, name);
        playerRepository.save(newUser);

        return newUser;
    }

    private boolean isValidUser(String username, String password, String name) {
        if (username == null || "".equals(username.trim()))
            return false;

        if (password == null || "".equals(password.trim()))
            return false;

        return name != null && !"".equals(name.trim());
    }

    public GameSession getCurrentGame(String username) {
        List<GameSession> games = gameSessionRepository.findByUsername(username);
        // get current active game, if there's no game -> create new one

        var activeGame = games.isEmpty()
                ? createGame(username)
                : games.stream()
                .filter(GameSession::getIsActive) //tim toi game true
                .findFirst()
                .orElseGet(() -> createGame(username));

        // get guess list and add to game
        activeGame.setGuess(guessRepository
                .findBySession(activeGame.getId()));

        return activeGame;
    }
    public GameSession getOldGame(String username,String gameId) {
        List<GameSession> games = gameSessionRepository.findByUsername(username);

        // vo hieu hoa cac game dang hoat dong
        gameSessionRepository.deactivateAllGames(username);

        // lay ra game id can hoat dong
        var activeGame = games.stream()
                .filter(s -> s.getId().equals(gameId))
                .findFirst().get();

        // set dang hoat dong
        activeGame.setActive(true);

        // update len server thay doi is_active = 1
        gameSessionRepository.updateActiveGames(activeGame);

        // get guess list and add to game
        activeGame.setGuess(guessRepository
                .findBySession(activeGame.getId()));

        return activeGame;
    }

    public void saveGuess(Guess guess) {
        guessRepository.save(guess);
    }

    public void skipAndPlayNewGame(String username) {
        createGame(username);
    }

    public GameSession getGameSession(String id) {
        GameSession gameSession = gameSessionRepository.findById(id);
        gameSession.setGuess(guessRepository.findBySession(id));
        return gameSession;
    }

    public void completeGame(String sessionId, LocalDateTime endTime) {
        gameSessionRepository.completeGame(sessionId,endTime);
    }

    public List<GameSession> getListRank() {
        List<GameSession> gameSessions = gameSessionRepository.findAllGameSession();
        // lay list game session da hoan thanh va sap xep lai totalTime
        return gameSessions.stream()
                .filter(GameSession::getIsCompleted)
                .sorted(Comparator.comparing(GameSession::getTotalTime)).collect(Collectors.toList());
    }
    public List<GameSession> getHistory(String username) {
        return gameSessionRepository.findByUsername(username);
    }


}
