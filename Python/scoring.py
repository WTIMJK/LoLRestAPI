import time

start = time.time()

import os
import sys
import pymongo
import cassiopeia as cass
import numpy as np
import pandas as pd
import pickle
from sklearn import svm
from sklearn import tree
from sklearn.model_selection import cross_val_score
import scipy.optimize

#CONSTANTS

STARTING_ITEMS = sorted(
("Boots of Speed","Faerie Charm","Rejuvenation Bead","Sapphire Crystal","Ruby Crystal","Cloth Armor","Null-Magic Mantle","Long Sword","Hunter's Talisman","Hunter's Machete","Dagger",
"Brawler's Gloves","Amplifying Tome","Doran's Shield","Doran's Blade","Doran's Ring","The Dark Seal","Cull","Ancient Coin","Relic Shield","Spellthief's Edge","Corrupting Potion"))

STARTING_ITEMS = {STARTING_ITEMS[i] : i for i in range(len(STARTING_ITEMS))}

SUMMONER_SPELLS = cass.get_summoner_spells("NA")
SUMMONER_SPELLS = sorted([spell.name for spell in SUMMONER_SPELLS if cass.GameMode.classic in spell.modes])
SUMMONER_SPELLS = {SUMMONER_SPELLS[i] : i for i in range(len(SUMMONER_SPELLS))}

POSITIONS = sorted(('TOP','MID','BOT','JUNGLE'))
POSITIONS = {POSITIONS[i] : i for i in range(len(POSITIONS))}
# CHAMPIONS = cass.get_champions("NA")
# CHAMPIONS = sorted([champion.name for champion in CHAMPIONS])
# CHAMPIONS = {CHAMPIONS[i] : i for i in range(len(CHAMPIONS))}

LABELS = ['BOTTOM', 'JUNGLE', 'MIDDLE', 'SUPPORT', 'TOP']

trainingColumns = ['CS','Exp','MonsterKills'] + list(SUMMONER_SPELLS.keys()) + list(POSITIONS.keys()) + \
list(STARTING_ITEMS.keys()) + ['ParticipantId', 'GameId','VerifiedRole']

#FUNCTIONS

def featureGetCS(participant):
    return [participant['creepScoreAt12']]

def featureGetStartingItems(participant):
    StartingItems = [0] * len(STARTING_ITEMS)
    if 'startingItems' in participant:
        for item in participant['startingItems']:
            if item in STARTING_ITEMS:
                StartingItems[STARTING_ITEMS[item]] = 1
    return StartingItems

# def featureGetChampion(participant):
#     champions = [0] * len(CHAMPIONS)
#     for champion in CHAMPIONS:
#         if participant['champion'] == champion:
#             champions[CHAMPIONS[participant['champion']]] = 1
#     return champions

def featureGetFrequentPosition(participant):
    positions = [0] * len(POSITIONS)
    for p in POSITIONS:
        if participant['frequentPosition'] == p:
            positions[POSITIONS[participant['frequentPosition']]] = 1
    return positions

def featureGetExperience(participant):
    return [participant['experienceAt12']]

def featureGetSummonerSpells(participant):
    spells = [0] * len(SUMMONER_SPELLS)
    for spell in participant['summonerSpells']:
        if spell in SUMMONER_SPELLS:
            spells[SUMMONER_SPELLS[spell]] = 1
    return spells

def featureGetNeutralKills(participant):
    return [participant['neutralKillsAt12']]

def getFeatures(participant):
    features = {
        "cs" : featureGetCS(participant),
        "exp" : featureGetExperience(participant),
        "monsters" : featureGetNeutralKills(participant),
        "spells" : featureGetSummonerSpells(participant),
        "position" : featureGetFrequentPosition(participant),
        "items" : featureGetStartingItems(participant)
        #"champion" : featureGetChampion(participant)
    }
    
    l = list(features.values())
    return [item for sublist in l for item in sublist]

def getGameData(game):
    data = []
    features = []
    gameId = game['_id']
    participants = game['participantData']
    for participant in participants.values():
        participantId = participant['participantId']
        if 'validatedRole' in game:
            verifiedRole = participant['validatedRole']
            features.append(verifiedRole)
        features = getFeatures(participant)
        data.append(features)        
    return data

def getData(data):
    rows = []
    for d in data:
        gameId = d['_id']
        participants = d['participantData']
        for participant in participants.values():
            participantId = participant['participantId']
            verifiedRole = participant['validatedRole']
            features = getFeatures(participant)
            features.append(participantId)
            features.append(gameId)
            features.append(verifiedRole)
            rows.append(features)
    return rows

def split_features_by_team(features, teamsize=5):
    teams = []
    assert len(features) % teamsize == 0
    for i in range(0, len(features), teamsize):
        teams.append(features[i:i+teamsize])
    return teams

def predict_team(team, clf, verbose=False):
    assert len(team) == len(LABELS)
    probs = 1 - clf.predict_proba(team)
    print(probs)
    row_ind, result = scipy.optimize.linear_sum_assignment(probs)
    cost = probs[row_ind, result].sum() / len(team)
    if verbose:
        print(probs)
        print(result)
    return [LABELS[i] for i in result], cost
def get_roles(gameId, coll, clf, INDEX, log):
    print(INDEX, gameId)
    gameLog = open("Python\\Logs\\{0}.txt".format(str(INDEX)), 'w')
    gameLog.write(str(gameId) + "\n")
    game = coll.find({'_id' : gameId})[0]
    gameData = getGameData(game)
    teams = split_features_by_team(gameData)
    roleList = []
    for team in teams:
        roles, cost = predict_team(team, clf)
        roleList+=roles
    print(roleList)
 
    for i in range(len(game['participantData'])):
        participants = game['participantData']
        participants[str(i+1)]['validatedRole']=roleList[i]
        participant = 'participantData.'+str(i+1)+'.validatedRole'
        query = {'_id' : gameId}
        values = {'$set': {participant : roleList[i]}}
        coll.update_one(query, values)
    log.write(str(gameId) + "\n")
    gameLog.write("Complete\n")
    gameLog.close()

def main():
    INDEX = 0
    log = open("Python\\modelLog.txt", 'w')
    modelLocation = 'Python\\finalized_model.sav'
    modelFile = open(modelLocation, 'rb')
    loaded_model = pickle.load(modelFile)
    collection = "match"
    myclient = pymongo.MongoClient("mongodb://localhost:27017/")
    mydb = myclient["LoLData"]
    coll = mydb[collection]
    gameFile = sys.argv[1]
    f = open(gameFile)
    count = 0
    for line in f.readlines():
        print(line)
        count += 1
        gameId = int(line)
        get_roles(gameId, coll, loaded_model, INDEX, log)
        INDEX += 1
    f.close()
    os.remove(gameFile)
    end = time.time()
    log.write(str(end - start) + " seconds to update " + str(count) + " games\n")
    log.close()
    sys.exit()
    
if __name__ == "__main__":
    main()
